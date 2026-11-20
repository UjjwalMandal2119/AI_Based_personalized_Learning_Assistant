package com.learningassistant.ai;

import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import com.learningassistant.common.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class DailyBriefService {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefService.class);

    private final DailyBriefRepository briefs;
    private final StudentRepository students;
    private final StudentContextService contextService;
    private final AiProviderRouter aiRouter;

    public DailyBriefService(DailyBriefRepository briefs,
                             StudentRepository students,
                             StudentContextService contextService,
                             AiProviderRouter aiRouter) {
        this.briefs = briefs;
        this.students = students;
        this.contextService = contextService;
        this.aiRouter = aiRouter;
    }

    @Transactional(readOnly = true)
    public Optional<DailyBrief> getToday(String email) {
        return briefs.findByStudent_EmailAndBriefDate(email, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<DailyBrief> getLatest(String email) {
        return briefs.findTopByStudent_EmailOrderByBriefDateDesc(email);
    }

    @Transactional
    public Optional<DailyBrief> getOrGenerateToday(String email) {
        Optional<DailyBrief> cached = getToday(email);
        if (cached.isPresent()) {
            return cached;
        }
        return generateToday(email);
    }

    @Transactional
    public Optional<DailyBrief> generateToday(String email) {
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));

        StudentContext context = contextService.build(email);
        String prompt = buildPrompt(student, context);

        AiGenerationResult result;
        try {
            result = aiRouter.generate(prompt);
        } catch (Exception e) {
            log.warn("Daily brief generation failed: {}", e.getMessage());
            return Optional.empty();
        }

        String reply = result.text();
        DailyBriefData data = parseBrief(reply);

        if (data == null) {
            log.warn("Failed to parse daily brief from AI response");
            return Optional.empty();
        }

        DailyBrief saved = briefs.save(new DailyBrief(
                student, LocalDate.now(),
                data.focusSummary(), data.priorityTasks(), data.revisionTopics(),
                data.dailyObjective(), data.recommendedActions(),
                result.providerName(), result.modelName(),
                result.durationMs(), result.fallbackUsed()
        ));
        return Optional.of(saved);
    }

    private String buildPrompt(Student student, StudentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personalized learning coach. Generate a concise daily learning brief ")
          .append("for ").append(student.getDisplayName()).append(" based on their actual data.\n\n");
        
        sb.append("CONTEXT:\n");
        if (context.hasActiveGoals()) {
            sb.append("Active Goals:\n");
            for (var g : context.activeGoals()) {
                sb.append("  - ").append(g.title()).append(" (").append(g.subject()).append(", ")
                  .append(g.topic()).append(", ").append(g.proficiencyLevel()).append(")\n");
            }
        }
        if (context.hasPendingTasks()) {
            sb.append("Pending Tasks (").append(context.pendingCount()).append("):\n");
            for (var t : context.pendingTasks().stream().limit(5).toList()) {
                sb.append("  - ").append(t.title()).append(" [").append(t.taskType())
                  .append(", ").append(t.estimatedMinutes()).append(" min");
                if (t.dueDate() != null) sb.append(", due ").append(t.dueDate());
                if (t.overdue()) sb.append(" ⚠ OVERDUE");
                sb.append("]\n");
            }
        }
        if (context.overdueCount() > 0) {
            sb.append("Overdue tasks: ").append(context.overdueCount()).append("\n");
        }
        if (context.hasCompletedTasks()) {
            sb.append("Recently completed: ").append(context.completedCount()).append(" tasks\n");
        }
        if (context.hasRecentLessons()) {
            sb.append("Recent lessons: ").append(context.recentLessons().size()).append("\n");
        }

        sb.append("\nGenerate a JSON object with these exact fields:\n")
          .append("focusSummary (string): 2-3 sentences on what to focus on today\n")
          .append("priorityTasks (string): Bullet list of 3-5 top priority tasks with why\n")
          .append("revisionTopics (string): Topics needing revision based on pending/overdue\n")
          .append("dailyObjective (string): One specific, achievable objective for today\n")
          .append("recommendedActions (array of strings): 3-5 actionable next steps\n")
          .append("Base ONLY on data above. No fabrication. If no data, say so and suggest starting a goal.\n")
          .append("Return ONLY the JSON object, no markdown, no explanation.");

        return sb.toString();
    }

    private DailyBriefData parseBrief(String text) {
        try {
            String clean = text.trim();
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, DailyBriefData.class);
        } catch (Exception e) {
            log.warn("Failed to parse daily brief JSON: {}", e.getMessage());
            return null;
        }
    }

    public record DailyBriefData(
            String focusSummary,
            String priorityTasks,
            String revisionTopics,
            String dailyObjective,
            java.util.List<String> recommendedActions
    ) {}
}