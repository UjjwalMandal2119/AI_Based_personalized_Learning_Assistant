CREATE TABLE ai_recommendations (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    learning_goal_id BINARY(16),
    title VARCHAR(200) NOT NULL,
    recommendation TEXT NOT NULL,
    reason TEXT NOT NULL,
    topic VARCHAR(160) NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    estimated_minutes INTEGER NOT NULL,
    suggested_actions JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ai_rec_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_rec_goal FOREIGN KEY (learning_goal_id)
        REFERENCES learning_goals(id) ON DELETE SET NULL,
    INDEX idx_ai_rec_student_created (student_id, created_at DESC)
) ENGINE=InnoDB;
