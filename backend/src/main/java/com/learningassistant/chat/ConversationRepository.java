package com.learningassistant.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findAllByStudent_EmailOrderByUpdatedAtDesc(String email);
    Optional<Conversation> findByIdAndStudent_Email(UUID id, String email);
}
