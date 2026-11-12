package com.learningassistant.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Routes AI generation requests: Ollama (primary) → Gemini (fallback).
 * Fallback is triggered only on retryable provider failures.
 */
@Component
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);

    private final OllamaAiProvider ollama;
    private final GeminiAiProvider gemini;
    private final boolean fallbackEnabled;

    public AiProviderRouter(OllamaAiProvider ollama, GeminiAiProvider gemini,
                            @Value("${ai.fallback.enabled:true}") boolean fallbackEnabled) {
        this.ollama = ollama;
        this.gemini = gemini;
        this.fallbackEnabled = fallbackEnabled;
    }

    /**
     * Generates text using the primary provider, falling back to the secondary
     * on retryable failures.
     *
     * @throws AiProviderException if both providers fail or fallback is disabled
     */
    public AiGenerationResult generate(String prompt) {
        long start = System.currentTimeMillis();
        try {
            String text = ollama.generateText(prompt);
            long ms = System.currentTimeMillis() - start;
            log.info("AI generation via {} ({}) completed in {}ms", ollama.getProviderName(), ollama.getModelName(), ms);
            return new AiGenerationResult(text, ollama.getProviderName(), ollama.getModelName(), ms, false);
        } catch (AiProviderException primaryFailure) {
            log.warn("Primary provider ({}) failed: {} — retryable={}",
                    ollama.getProviderName(), primaryFailure.getMessage(), primaryFailure.isRetryable());
            if (!primaryFailure.isRetryable() || !fallbackEnabled) {
                throw primaryFailure;
            }
            return tryFallback(prompt, start);
        }
    }

    private AiGenerationResult tryFallback(String prompt, long start) {
        if (!gemini.isAvailable()) {
            log.warn("Fallback provider ({}) is not available", gemini.getProviderName());
            throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Both AI providers are unavailable. Please try again later.", false);
        }
        log.info("Activating fallback provider: {}", gemini.getProviderName());
        try {
            String text = gemini.generateText(prompt);
            long ms = System.currentTimeMillis() - start;
            log.info("Fallback AI generation via {} ({}) completed in {}ms",
                    gemini.getProviderName(), gemini.getModelName(), ms);
            return new AiGenerationResult(text, gemini.getProviderName(), gemini.getModelName(), ms, true);
        } catch (AiProviderException fallbackFailure) {
            log.warn("Fallback provider ({}) also failed: {}", gemini.getProviderName(), fallbackFailure.getMessage());
            throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Both AI providers failed. Please try again later.", false, fallbackFailure);
        }
    }

    public String getPrimaryProviderName() { return ollama.getProviderName(); }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public boolean isOllamaAvailable() { return ollama.isAvailable(); }
    public boolean isGeminiAvailable() { return gemini.isAvailable(); }
}
