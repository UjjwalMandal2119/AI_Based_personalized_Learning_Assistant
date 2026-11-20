package com.learningassistant.ai;

import com.learningassistant.learninggoal.LearningGoalDtos;
import com.learningassistant.task.TaskDtos;

import java.time.Instant;
import java.util.List;

/**
 * Compact, structured snapshot of an authenticated student's learning state.
 *
 * <p>Only real persisted data is included. Fields are empty lists when no data
 * exists — the AI coach must never fabricate marks, streaks, or completed
 * activity.</p>
 */
public record StudentContext(
        String displayName,
        List<LearningGoalDtos.GoalResponse> activeGoals,
        List<TaskSummary> pendingTasks,
        List<TaskSummary> completedTasks,
        List<LessonSummary> recentLessons,
        List<RecommendationSummary> recentRecommendations,
        long pendingCount,
        long completedCount,
        long overdueCount
) {

    /** Minimal task projection sent to the LLM. */
    public record TaskSummary(
            String title,
            String subject,
            String topic,
            String taskType,
            int estimatedMinutes,
            String dueDate,
            boolean overdue
    ) {}

    /** Minimal lesson projection sent to the LLM. */
    public record LessonSummary(
            String title,
            String topic,
            String difficulty,
            int estimatedStudyMinutes,
            String createdAt
    ) {}

    /** Minimal recommendation projection sent to the LLM. */
    public record RecommendationSummary(
            String title,
            String recommendation,
            String reason,
            String topic,
            String activityType,
            int estimatedMinutes,
            String providerName,
            boolean fallbackUsed
    ) {}

    public boolean hasActiveGoals() {
        return activeGoals != null && !activeGoals.isEmpty();
    }

    public boolean hasPendingTasks() {
        return pendingTasks != null && !pendingTasks.isEmpty();
    }

    public boolean hasCompletedTasks() {
        return completedTasks != null && !completedTasks.isEmpty();
    }

    public boolean hasRecentLessons() {
        return recentLessons != null && !recentLessons.isEmpty();
    }

    public boolean hasRecentRecommendations() {
        return recentRecommendations != null && !recentRecommendations.isEmpty();
    }
}