package com.learningassistant.learninggoal;

import com.learningassistant.student.Student;
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
@Table(name = "learning_goals")
public class LearningGoal {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(nullable = false, length = 120)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 20)
    private ProficiencyLevel proficiencyLevel;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status = GoalStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LearningGoal() {}

    public LearningGoal(Student student, String title, String subject, String topic,
                        ProficiencyLevel proficiencyLevel, LocalDate targetDate) {
        this.student = student;
        this.title = title;
        this.subject = subject;
        this.topic = topic;
        this.proficiencyLevel = proficiencyLevel;
        this.targetDate = targetDate;
    }

    public UUID getId() { return id; }
    public Student getStudent() { return student; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public ProficiencyLevel getProficiencyLevel() { return proficiencyLevel; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String title, String subject, String topic, ProficiencyLevel proficiencyLevel,
                       LocalDate targetDate, GoalStatus status) {
        this.title = title;
        this.subject = subject;
        this.topic = topic;
        this.proficiencyLevel = proficiencyLevel;
        this.targetDate = targetDate;
        this.status = status;
    }
}
