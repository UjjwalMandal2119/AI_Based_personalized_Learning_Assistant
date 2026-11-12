package com.learningassistant.ai;

/**
 * Common abstraction for AI text-generation providers.
 * Implementations must be stateless and thread-safe.
 */
public interface AiProvider {

    /** Human-readable provider name used in logs and persisted metadata. */
    String getProviderName();

    /** Model identifier used for this provider instance. */
    String getModelName();

    /**
     * Returns true when the provider is configured and reachable.
     * Must be cheap — no model-generation requests.
     */
    boolean isAvailable();

    /**
     * Generates a plain-text response for the given prompt.
     *
     * @throws AiProviderException on provider-level failures (timeout, unavailable, bad response)
     */
    String generateText(String prompt);
}
