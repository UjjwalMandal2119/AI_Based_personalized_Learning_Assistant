package com.learningassistant.task;

import com.learningassistant.learninggoal.LearningGoal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "study_tasks")
public class StudyTask {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_goal_id", nullable = false)
    private LearningGoal learningGoal;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean completed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected StudyTask() {}

    public StudyTask(LearningGoal learningGoal, String title, String description,
                     TaskType taskType, int estimatedMinutes, LocalDate dueDate) {
        this.learningGoal = learningGoal;
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.estimatedMinutes = estimatedMinutes;
        this.dueDate = dueDate;
    }

    public UUID getId() { return id; }
    public LearningGoal getLearningGoal() { return learningGoal; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskType getTaskType() { return taskType; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isCompleted() { return completed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void update(String title, String description, TaskType taskType,
                       int estimatedMinutes, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.estimatedMinutes = estimatedMinutes;
        this.dueDate = dueDate;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.completedAt = completed ? Instant.now() : null;
    }
}
