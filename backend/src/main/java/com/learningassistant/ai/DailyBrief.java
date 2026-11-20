package com.learningassistant.ai;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cached daily learning brief for a student.
 * Generated once per day, refreshed on demand.
 */
@Entity
@Table(name = "daily_briefs", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "brief_date"}))
public class DailyBrief {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private com.learningassistant.student.Student student;

    @Column(name = "brief_date", nullable = false)
    private java.time.LocalDate briefDate;

    @Column(name = "focus_summary", columnDefinition = "TEXT")
    private String focusSummary;

    @Column(name = "priority_tasks", columnDefinition = "TEXT")
    private String priorityTasks;

    @Column(name = "revision_topics", columnDefinition = "TEXT")
    private String revisionTopics;

    @Column(name = "daily_objective", columnDefinition = "TEXT")
    private String dailyObjective;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommended_actions", columnDefinition = "json")
    private List<String> recommendedActions;

    @Column(name = "provider_name", length = 30)
    private String providerName;

    @Column(name = "model_name", length = 80)
    private String modelName;

    @Column(name = "generation_duration_ms")
    private Long generationDurationMs;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DailyBrief() {}

    public DailyBrief(com.learningassistant.student.Student student, java.time.LocalDate briefDate,
                      String focusSummary, String priorityTasks, String revisionTopics,
                      String dailyObjective, List<String> recommendedActions,
                      String providerName, String modelName, Long generationDurationMs,
                      boolean fallbackUsed) {
        this.student = student;
        this.briefDate = briefDate;
        this.focusSummary = focusSummary;
        this.priorityTasks = priorityTasks;
        this.revisionTopics = revisionTopics;
        this.dailyObjective = dailyObjective;
        this.recommendedActions = recommendedActions;
        this.providerName = providerName;
        this.modelName = modelName;
        this.generationDurationMs = generationDurationMs;
        this.fallbackUsed = fallbackUsed;
    }

    public UUID getId() { return id; }
    public com.learningassistant.student.Student getStudent() { return student; }
    public java.time.LocalDate getBriefDate() { return briefDate; }
    public String getFocusSummary() { return focusSummary; }
    public String getPriorityTasks() { return priorityTasks; }
    public String getRevisionTopics() { return revisionTopics; }
    public String getDailyObjective() { return dailyObjective; }
    public List<String> getRecommendedActions() { return recommendedActions; }
    public String getProviderName() { return providerName; }
    public String getModelName() { return modelName; }
    public Long getGenerationDurationMs() { return generationDurationMs; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public Instant getCreatedAt() { return createdAt; }
}