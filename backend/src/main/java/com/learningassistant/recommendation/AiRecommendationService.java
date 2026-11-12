package com.learningassistant.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.learninggoal.GoalStatus;
import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.learninggoal.LearningGoalRepository;
import com.learningassistant.lesson.LessonGenerationException;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import com.learningassistant.task.StudyTask;
import com.learningassistant.task.StudyTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);
    /** Reuse a cached recommendation if it is less than this many hours old. */
    private static final long CACHE_HOURS = 6;

    private final StudentRepository students;
    private final LearningGoalRepository goals;
    private final StudyTaskRepository tasks;
    private final AiRecommendationRepository recommendations;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AiRecommendationService(StudentRepository students, LearningGoalRepository goals,
                                   StudyTaskRepository tasks, AiRecommendationRepository recommendations,
                                   RestClient geminiRestClient, ObjectMapper objectMapper,
                                   @Value("${gemini.api-key:}") String apiKey,
                                   @Value("${gemini.model:gemini-3.8-flash}") String model) {
        this.students = students;
        this.goals = goals;
        this.tasks = tasks;
        this.recommendations = recommendations;
        this.restClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    /** Returns the most recent cached recommendation, or empty if none exists. */
    @Transactional(readOnly = true)
    public Optional<AiRecommendationDtos.RecommendationResponse> getLatest(String email) {
        return recommendations.findTopByStudent_EmailOrderByCreatedAtDesc(email)
                .map(AiRecommendationDtos.RecommendationResponse::from);
    }

    /**
     * Generates a new recommendation if the cached one is stale (older than CACHE_HOURS),
     * otherwise returns the cached one. Falls back gracefully if Gemini is unavailable.
     */
    @Transactional
    public Optional<AiRecommendationDtos.RecommendationResponse> getOrGenerate(String email) {
        Optional<AiRecommendation> cached = recommendations.findTopByStudent_EmailOrderByCreatedAtDesc(email);
        if (cached.isPresent() && cached.get().getCreatedAt()
                .isAfter(Instant.now().minus(CACHE_HOURS, ChronoUnit.HOURS))) {
            return cached.map(AiRecommendationDtos.RecommendationResponse::from);
        }
        return generate(email);
    }

    /** Always generates a fresh recommendation from Gemini and persists it. */
    @Transactional
    public Optional<AiRecommendationDtos.RecommendationResponse> generate(String email) {
        if (apiKey.isBlank()) {
            log.info("Gemini API key not configured — skipping recommendation generation");
            return Optional.empty();
        }
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
        List<LearningGoal> activeGoals =
                goals.findAllByStudent_EmailAndStatusOrderByCreatedAtDesc(email, GoalStatus.ACTIVE);
        if (activeGoals.isEmpty()) {
            return Optional.empty();
        }
        List<StudyTask> allTasks = tasks.findAllByLearningGoal_Student_EmailOrderByDueDateAsc(email);
        String prompt = buildPrompt(student, activeGoals, allTasks);
        try {
            AiRecommendationDtos.GeneratedRecommendation generated = callGemini(prompt);
            if (!isValid(generated)) {
                log.warn("Gemini returned an invalid recommendation structure — skipping persistence");
                return Optional.empty();
            }
            LearningGoal primaryGoal = activeGoals.get(0);
            AiRecommendation saved = recommendations.save(new AiRecommendation(
                    student, primaryGoal,
                    generated.title(), generated.recommendation(), generated.reason(),
                    generated.topic(), generated.activityType(), generated.estimatedMinutes(),
                    generated.suggestedActions()));
            return Optional.of(AiRecommendationDtos.RecommendationResponse.from(saved));
        } catch (LessonGenerationException exception) {
            log.warn("Gemini recommendation generation failed: {}", exception.getMessage());
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("Unexpected error during recommendation generation", exception);
            return Optional.empty();
        }
    }

    private AiRecommendationDtos.GeneratedRecommendation callGemini(String prompt) {
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", recommendationSchema(),
                        "maxOutputTokens", 1024,
                        "temperature", 0.5));
        try {
            JsonNode response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(response);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 429) {
                throw new LessonGenerationException(HttpStatus.TOO_MANY_REQUESTS,
                        "Gemini rate limit reached", exception);
            }
            throw new LessonGenerationException(HttpStatus.BAD_GATEWAY,
                    "Gemini request failed", exception);
        } catch (ResourceAccessException exception) {
            throw new LessonGenerationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini is temporarily unavailable", exception);
        }
    }

    private AiRecommendationDtos.GeneratedRecommendation parseResponse(JsonNode response) {
        JsonNode textNode = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode == null || !textNode.isTextual() || textNode.asText().isBlank()) {
            throw new LessonGenerationException(HttpStatus.BAD_GATEWAY, "Empty Gemini response");
        }
        try {
            return objectMapper.readValue(textNode.asText(),
                    AiRecommendationDtos.GeneratedRecommendation.class);
        } catch (JsonProcessingException exception) {
            throw new LessonGenerationException(HttpStatus.BAD_GATEWAY,
                    "Malformed Gemini recommendation JSON", exception);
        }
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
        sb.append("\nGenerate a focused, encouraging recommendation based only on this data. ")
          .append("Do not invent scores or history not present above. ")
          .append("Return only the JSON object matching the required schema.");
        return sb.toString();
    }

    private Map<String, Object> recommendationSchema() {
        Map<String, Object> str = Map.of("type", "STRING");
        return Map.of("type", "OBJECT",
                "properties", Map.of(
                        "title", str,
                        "recommendation", str,
                        "reason", str,
                        "topic", str,
                        "activityType", str,
                        "estimatedMinutes", Map.of("type", "INTEGER"),
                        "suggestedActions", Map.of("type", "ARRAY", "items", str)),
                "required", List.of("title", "recommendation", "reason", "topic",
                        "activityType", "estimatedMinutes", "suggestedActions"));
    }
}
