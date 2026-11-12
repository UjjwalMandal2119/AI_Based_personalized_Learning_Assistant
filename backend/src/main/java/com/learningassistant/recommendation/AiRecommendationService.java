package com.learningassistant.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.ai.AiGenerationResult;
import com.learningassistant.ai.AiProviderException;
import com.learningassistant.ai.AiProviderRouter;
import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.learninggoal.GoalStatus;
import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.learninggoal.LearningGoalRepository;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import com.learningassistant.task.StudyTask;
import com.learningassistant.task.StudyTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);
    private static final long CACHE_HOURS = 6;

    private final StudentRepository students;
    private final LearningGoalRepository goals;
    private final StudyTaskRepository tasks;
    private final AiRecommendationRepository recommendations;
    private final AiProviderRouter aiRouter;
    private final ObjectMapper objectMapper;

    public AiRecommendationService(StudentRepository students, LearningGoalRepository goals,
                                   StudyTaskRepository tasks, AiRecommendationRepository recommendations,
                                   AiProviderRouter aiRouter, ObjectMapper objectMapper) {
        this.students = students;
        this.goals = goals;
        this.tasks = tasks;
        this.recommendations = recommendations;
        this.aiRouter = aiRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<AiRecommendationDtos.RecommendationResponse> getLatest(String email) {
        return recommendations.findTopByStudent_EmailOrderByCreatedAtDesc(email)
                .map(AiRecommendationDtos.RecommendationResponse::from);
    }

    @Transactional
    public Optional<AiRecommendationDtos.RecommendationResponse> getOrGenerate(String email) {
        Optional<AiRecommendation> cached =
                recommendations.findTopByStudent_EmailOrderByCreatedAtDesc(email);
        if (cached.isPresent() && cached.get().getCreatedAt()
                .isAfter(Instant.now().minus(CACHE_HOURS, ChronoUnit.HOURS))) {
            return cached.map(AiRecommendationDtos.RecommendationResponse::from);
        }
        return generate(email);
    }

    @Transactional
    public Optional<AiRecommendationDtos.RecommendationResponse> generate(String email) {
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
        List<LearningGoal> activeGoals =
                goals.findAllByStudent_EmailAndStatusOrderByCreatedAtDesc(email, GoalStatus.ACTIVE);
        if (activeGoals.isEmpty()) {
            return Optional.empty();
        }
        List<StudyTask> allTasks = tasks.findAllByLearningGoal_Student_EmailOrderByDueDateAsc(email);
        String prompt = buildPrompt(student, activeGoals, allTasks);

        AiGenerationResult result;
        try {
            result = aiRouter.generate(prompt);
        } catch (AiProviderException e) {
            log.warn("AI recommendation generation failed ({}): {}", e.getStatus(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error during recommendation generation", e);
            return Optional.empty();
        }

        AiRecommendationDtos.GeneratedRecommendation generated;
        try {
            generated = parseJson(result.text());
        } catch (JsonProcessingException e) {
            log.warn("AI provider ({}) returned unparseable recommendation JSON", result.providerName());
            return Optional.empty();
        }

        if (!isValid(generated)) {
            log.warn("AI provider ({}) returned invalid recommendation structure", result.providerName());
            return Optional.empty();
        }

        LearningGoal primaryGoal = activeGoals.get(0);
        AiRecommendation saved = recommendations.save(new AiRecommendation(
                student, primaryGoal,
                generated.title(), generated.recommendation(), generated.reason(),
                generated.topic(), generated.activityType(), generated.estimatedMinutes(),
                generated.suggestedActions(),
                result.providerName(), result.modelName(),
                result.durationMs(), result.fallbackUsed()));
        return Optional.of(AiRecommendationDtos.RecommendationResponse.from(saved));
    }

    private AiRecommendationDtos.GeneratedRecommendation parseJson(String text)
            throws JsonProcessingException {
        // Strip markdown code fences that some models add
        String clean = text.trim();
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        return objectMapper.readValue(clean, AiRecommendationDtos.GeneratedRecommendation.class);
    }

    private boolean isValid(AiRecommendationDtos.GeneratedRecommendation rec) {
        return rec != null
                && rec.title() != null && !rec.title().isBlank()
                && rec.recommendation() != null && !rec.recommendation().isBlank()
                && rec.reason() != null && !rec.reason().isBlank()
                && rec.topic() != null && !rec.topic().isBlank()
                && rec.activityType() != null && !rec.activityType().isBlank()
                && rec.estimatedMinutes() > 0
                && rec.suggestedActions() != null && !rec.suggestedActions().isEmpty();
    }

    private String buildPrompt(Student student, List<LearningGoal> activeGoals,
                                List<StudyTask> allTasks) {
        long completed = allTasks.stream().filter(StudyTask::isCompleted).count();
        long pending = allTasks.stream().filter(t -> !t.isCompleted()).count();
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personalized learning coach. Based on the student's actual learning data below, ")
          .append("generate one specific, actionable study recommendation.\n\n")
          .append("Student: ").append(student.getDisplayName()).append("\n")
          .append("Active learning goals:\n");
        for (LearningGoal goal : activeGoals) {
            sb.append("  - ").append(goal.getTitle())
              .append(" (Subject: ").append(goal.getSubject())
              .append(", Topic: ").append(goal.getTopic())
              .append(", Level: ").append(goal.getProficiencyLevel().name()).append(")\n");
        }
        sb.append("Tasks completed: ").append(completed).append("\n");
        sb.append("Tasks pending: ").append(pending).append("\n");
        List<StudyTask> recentPending = allTasks.stream()
                .filter(t -> !t.isCompleted()).limit(3).toList();
        if (!recentPending.isEmpty()) {
            sb.append("Next pending tasks:\n");
            for (StudyTask task : recentPending) {
                sb.append("  - ").append(task.getTitle())
                  .append(" (").append(task.getTaskType().name()).append(", ")
                  .append(task.getEstimatedMinutes()).append(" min)\n");
            }
        }
        sb.append("\nRespond with ONLY a JSON object (no markdown, no explanation) with these exact fields: ")
          .append("title (string), recommendation (string), reason (string), topic (string), ")
          .append("activityType (string), estimatedMinutes (integer), suggestedActions (array of strings). ")
          .append("Base the recommendation only on the data above. Do not invent scores or history.");
        return sb.toString();
    }
}
