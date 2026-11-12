CREATE TABLE conversations (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    subject VARCHAR(80),
    topic VARCHAR(120),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_conversations_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_conversations_student_updated (student_id, updated_at DESC)
) ENGINE=InnoDB;

CREATE TABLE chat_messages (
    id BINARY(16) PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    provider_name VARCHAR(30),
    model_name VARCHAR(80),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_chat_messages_conversation_created (conversation_id, created_at ASC)
) ENGINE=InnoDB;
