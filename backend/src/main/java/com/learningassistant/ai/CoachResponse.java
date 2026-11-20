package com.learningassistant.ai;

import com.learningassistant.chat.ChatDtos;

import java.util.List;

/**
 * Structured response from the AI coach. The LLM never returns raw text —
 * every reply is validated and shaped into this structure by the backend.
 */
public record CoachResponse(
        String conversationId,
        String reply,
        String mode,
        List<String> suggestedQuestions,
        List<CoachAction> actions,
        CoachMetadata metadata
) {
    public record CoachAction(
            String type,
            String label,
            String payload
    ) {}

    public record CoachMetadata(
            String provider,
            String model,
            long durationMs,
            boolean fallbackUsed
    ) {}

    /** Request body for the coach endpoint. */
    public record CoachRequest(
            String conversationId,
            String message,
            String mode,
            List<ChatDtos.MessageResponse> history
    ) {}
}