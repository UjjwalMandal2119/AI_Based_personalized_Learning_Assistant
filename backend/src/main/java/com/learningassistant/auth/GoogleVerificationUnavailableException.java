package com.learningassistant.auth;

public class GoogleVerificationUnavailableException extends RuntimeException {
    public GoogleVerificationUnavailableException(String message) {
        super(message);
    }

    public GoogleVerificationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
