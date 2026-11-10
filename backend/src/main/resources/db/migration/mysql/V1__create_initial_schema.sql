CREATE TABLE students (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE learning_goals (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    title VARCHAR(120) NOT NULL,
    subject VARCHAR(80) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    proficiency_level VARCHAR(20) NOT NULL CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    target_date DATE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_learning_goals_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_learning_goals_student_created (student_id, created_at DESC)
) ENGINE=InnoDB;

CREATE TABLE study_tasks (
    id BINARY(16) PRIMARY KEY,
    learning_goal_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    task_type VARCHAR(20) NOT NULL CHECK (task_type IN ('STUDY', 'PRACTICE', 'REVIEW', 'ASSIGNMENT')),
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes BETWEEN 1 AND 1440),
    due_date DATE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    CONSTRAINT fk_study_tasks_learning_goal FOREIGN KEY (learning_goal_id)
        REFERENCES learning_goals(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_completion_time CHECK (
        (completed = TRUE AND completed_at IS NOT NULL) OR
        (completed = FALSE AND completed_at IS NULL)
    ),
    INDEX idx_study_tasks_goal_due (learning_goal_id, due_date),
    INDEX idx_study_tasks_due_date (completed, due_date),
    INDEX idx_study_tasks_completed_at (completed, completed_at DESC)
) ENGINE=InnoDB;
