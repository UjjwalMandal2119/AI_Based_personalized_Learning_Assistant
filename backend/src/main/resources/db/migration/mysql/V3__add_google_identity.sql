ALTER TABLE students
    ADD COLUMN google_subject VARCHAR(255) NULL,
    ADD CONSTRAINT uk_students_google_subject UNIQUE (google_subject);
