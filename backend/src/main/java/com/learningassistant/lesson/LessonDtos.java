package com.learningassistant.lesson;

import java.time.Instant;
import java.util.UUID;

public final class LessonDtos {
    private LessonDtos() {}

    public record LessonResponse(
            UUID id,
            UUID learningGoalId,
            UUID studentId,
            String title,
            String topic,
            String difficulty,
            LessonContent content,
            int estimatedStudyMinutes,
            Instant createdAt) {

        public static LessonResponse from(LearningLesson lesson) {
            return new LessonResponse(lesson.getId(), lesson.getLearningGoal().getId(),
                    lesson.getStudent().getId(), lesson.getTitle(), lesson.getTopic(),
                    lesson.getDifficulty(), lesson.getContent(),
                    lesson.getEstimatedStudyMinutes(), lesson.getCreatedAt());
        }
    }
}
