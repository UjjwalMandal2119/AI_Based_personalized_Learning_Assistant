package com.learningassistant.recommendation;

import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
public class AiRecommendation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_goal_id")
    private LearningGoal learningGoal;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 160)
    private String topic;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_actions", nullable = false, columnDefinition = "json")
    private List<String> suggestedActions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiRecommendation() {}

    public AiRecommendation(Student student, LearningGoal learningGoal, String title,
                             String recommendation, String reason, String topic,
                             String activityType, int estimatedMinutes, List<String> suggestedActions) {
        this.student = student;
        this.learningGoal = learningGoal;
        this.title = title;
        this.recommendation = recommendation;
        this.reason = reason;
        this.topic = topic;
        this.activityType = activityType;
        this.estimatedMinutes = estimatedMinutes;
        this.suggestedActions = suggestedActions;
    }

    public UUID getId() { return id; }
    public Student getStudent() { return student; }
    public LearningGoal getLearningGoal() { return learningGoal; }
    public String getTitle() { return title; }
    public String getRecommendation() { return recommendation; }
    public String getReason() { return reason; }
    public String getTopic() { return topic; }
    public String getActivityType() { return activityType; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public List<String> getSuggestedActions() { return suggestedActions; }
    public Instant getCreatedAt() { return createdAt; }
}
