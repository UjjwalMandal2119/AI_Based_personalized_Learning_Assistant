package com.learningassistant.ai;

import org.springframework.http.HttpStatus;

/**
 * Thrown by an {@link AiProvider} when the provider itself fails
 * (connection error, timeout, rate-limit, bad response).
 * The {@link AiProviderRouter} catches this to activate fallback.
 */
public class AiProviderException extends RuntimeException {

    private final HttpStatus status;
    private final boolean retryable;

    public AiProviderException(HttpStatus status, String message, boolean retryable) {
        super(message);
        this.status = status;
        this.retryable = retryable;
    }

    public AiProviderException(HttpStatus status, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.retryable = retryable;
    }

    public HttpStatus getStatus() { return status; }

    /** True when the failure is transient and fallback should be attempted. */
    public boolean isRetryable() { return retryable; }
}
