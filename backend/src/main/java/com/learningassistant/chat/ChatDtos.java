package com.learningassistant.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDtos {
    private ChatDtos() {}

    public record CreateConversationRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 80) String subject,
            @Size(max = 120) String topic) {}

    public record RenameConversationRequest(
            @NotBlank @Size(max = 200) String title) {}

    public record SendMessageRequest(
            @NotBlank @Size(max = 4000) String content) {}

    public record ConversationSummary(UUID id, String title, String subject, String topic,
                                      Instant createdAt, Instant updatedAt, int messageCount) {}

    public record ConversationDetail(UUID id, String title, String subject, String topic,
                                     Instant createdAt, Instant updatedAt,
                                     List<MessageResponse> messages) {}

    public record MessageResponse(UUID id, String role, String content,
                                  String providerName, String modelName, Instant createdAt) {
        static MessageResponse from(ChatMessage m) {
            return new MessageResponse(m.getId(), m.getRole(), m.getContent(),
                    m.getProviderName(), m.getModelName(), m.getCreatedAt());
        }
    }
}
