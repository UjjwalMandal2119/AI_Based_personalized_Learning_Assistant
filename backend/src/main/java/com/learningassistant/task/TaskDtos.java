package com.learningassistant.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TaskDtos {
    private TaskDtos() {}

    public record TaskRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 2000) String description,
            @NotNull TaskType taskType,
            @Min(1) @Max(1440) int estimatedMinutes,
            LocalDate dueDate) {}

    public record CompletionRequest(boolean completed) {}

    public record TaskResponse(UUID id, UUID learningGoalId, String learningGoalTitle, String title,
                               String description, TaskType taskType, int estimatedMinutes,
                               LocalDate dueDate, boolean completed, Instant createdAt,
                               Instant completedAt) {
        public static TaskResponse from(StudyTask task) {
            return new TaskResponse(task.getId(), task.getLearningGoal().getId(),
                    task.getLearningGoal().getTitle(), task.getTitle(), task.getDescription(),
                    task.getTaskType(), task.getEstimatedMinutes(), task.getDueDate(),
                    task.isCompleted(), task.getCreatedAt(), task.getCompletedAt());
        }
    }
}
