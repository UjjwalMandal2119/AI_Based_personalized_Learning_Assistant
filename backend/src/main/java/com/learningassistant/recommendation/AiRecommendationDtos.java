package com.learningassistant.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiRecommendationDtos {
    private AiRecommendationDtos() {}

    public record RecommendationResponse(
            UUID id,
            UUID learningGoalId,
            String title,
            String recommendation,
            String reason,
            String topic,
            String activityType,
            int estimatedMinutes,
            List<String> suggestedActions,
            String providerName,
            boolean fallbackUsed,
            Instant createdAt) {

        public static RecommendationResponse from(AiRecommendation rec) {
            return new RecommendationResponse(
                    rec.getId(),
                    rec.getLearningGoal() != null ? rec.getLearningGoal().getId() : null,
                    rec.getTitle(), rec.getRecommendation(), rec.getReason(),
                    rec.getTopic(), rec.getActivityType(), rec.getEstimatedMinutes(),
                    rec.getSuggestedActions(), rec.getProviderName(), rec.isFallbackUsed(),
                    rec.getCreatedAt());
        }
    }

    /** Parsed from Gemini JSON output. */
    public record GeneratedRecommendation(
            String title,
            String recommendation,
            String reason,
            String topic,
            String activityType,
            int estimatedMinutes,
            List<String> suggestedActions) {}
}
