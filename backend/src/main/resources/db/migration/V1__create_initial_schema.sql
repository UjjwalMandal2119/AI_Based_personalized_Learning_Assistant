CREATE TABLE students (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE learning_goals (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    subject VARCHAR(80) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    proficiency_level VARCHAR(20) NOT NULL CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    target_date DATE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_learning_goals_student_created ON learning_goals(student_id, created_at DESC);

CREATE TABLE study_tasks (
    id UUID PRIMARY KEY,
    learning_goal_id UUID NOT NULL REFERENCES learning_goals(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    task_type VARCHAR(20) NOT NULL CHECK (task_type IN ('STUDY', 'PRACTICE', 'REVIEW', 'ASSIGNMENT')),
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes BETWEEN 1 AND 1440),
    due_date DATE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_task_completion_time CHECK (
        (completed = TRUE AND completed_at IS NOT NULL) OR
        (completed = FALSE AND completed_at IS NULL)
    )
);
CREATE INDEX idx_study_tasks_goal_due ON study_tasks(learning_goal_id, due_date);
CREATE INDEX idx_study_tasks_due_date ON study_tasks(due_date) WHERE completed = FALSE;
CREATE INDEX idx_study_tasks_completed_at ON study_tasks(completed_at DESC) WHERE completed = TRUE;
