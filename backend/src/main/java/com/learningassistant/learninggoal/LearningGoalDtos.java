package com.learningassistant.learninggoal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class LearningGoalDtos {
    private LearningGoalDtos() {}

    public record GoalRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 80) String subject,
            @NotBlank @Size(max = 120) String topic,
            @NotNull ProficiencyLevel proficiencyLevel,
            LocalDate targetDate,
            GoalStatus status) {}

    public record GoalResponse(UUID id, UUID studentId, String title, String subject, String topic,
                               ProficiencyLevel proficiencyLevel, LocalDate targetDate,
                               GoalStatus status, Instant createdAt) {
        public static GoalResponse from(LearningGoal goal) {
            return new GoalResponse(goal.getId(), goal.getStudent().getId(), goal.getTitle(),
                    goal.getSubject(), goal.getTopic(), goal.getProficiencyLevel(),
                    goal.getTargetDate(), goal.getStatus(), goal.getCreatedAt());
        }
    }
}
