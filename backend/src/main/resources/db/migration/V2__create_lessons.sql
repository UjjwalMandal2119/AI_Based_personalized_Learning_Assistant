CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    learning_goal_id UUID NOT NULL REFERENCES learning_goals(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    content JSONB NOT NULL,
    estimated_study_minutes INTEGER NOT NULL CHECK (estimated_study_minutes BETWEEN 1 AND 240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lessons_goal_created ON lessons(learning_goal_id, created_at DESC);
CREATE INDEX idx_lessons_student_created ON lessons(student_id, created_at DESC);
