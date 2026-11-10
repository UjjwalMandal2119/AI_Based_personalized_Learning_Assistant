CREATE TABLE lessons (
    id BINARY(16) PRIMARY KEY,
    learning_goal_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    content JSON NOT NULL,
    estimated_study_minutes INTEGER NOT NULL CHECK (estimated_study_minutes BETWEEN 1 AND 240),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_lessons_learning_goal FOREIGN KEY (learning_goal_id)
        REFERENCES learning_goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_lessons_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_lessons_goal_created (learning_goal_id, created_at DESC),
    INDEX idx_lessons_student_created (student_id, created_at DESC)
) ENGINE=InnoDB;
