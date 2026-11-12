package com.learningassistant.ai;

/**
 * Result of a successful AI text generation, including provider metadata
 * for persistence and observability.
 */
public record AiGenerationResult(
        String text,
        String providerName,
        String modelName,
        long durationMs,
        boolean fallbackUsed) {}
