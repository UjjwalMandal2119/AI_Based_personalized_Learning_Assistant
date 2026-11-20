-- V8: Create daily_briefs table for AI daily learning briefs
CREATE TABLE daily_briefs (
    id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    brief_date DATE NOT NULL,
    focus_summary TEXT,
    priority_tasks TEXT,
    revision_topics TEXT,
    daily_objective TEXT,
    recommended_actions JSON,
    provider_name VARCHAR(30),
    model_name VARCHAR(80),
    generation_duration_ms BIGINT,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_daily_briefs_student
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uk_daily_briefs_student_date
        UNIQUE (student_id, brief_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_daily_briefs_student_date ON daily_briefs (student_id, brief_date DESC);