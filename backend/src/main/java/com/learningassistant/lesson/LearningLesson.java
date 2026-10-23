package com.learningassistant.lesson;

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
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class LearningLesson {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_goal_id", nullable = false)
    private LearningGoal learningGoal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private LessonContent content;

    @Column(name = "estimated_study_minutes", nullable = false)
    private int estimatedStudyMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LearningLesson() {}

    public LearningLesson(LearningGoal learningGoal, Student student, String title, String topic,
                          String difficulty, LessonContent content, int estimatedStudyMinutes) {
        this.learningGoal = learningGoal;
        this.student = student;
        this.title = title;
        this.topic = topic;
        this.difficulty = difficulty;
        this.content = content;
        this.estimatedStudyMinutes = estimatedStudyMinutes;
    }

    public UUID getId() { return id; }
    public LearningGoal getLearningGoal() { return learningGoal; }
    public Student getStudent() { return student; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getDifficulty() { return difficulty; }
    public LessonContent getContent() { return content; }
    public int getEstimatedStudyMinutes() { return estimatedStudyMinutes; }
    public Instant getCreatedAt() { return createdAt; }
}
