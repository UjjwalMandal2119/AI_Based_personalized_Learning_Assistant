package com.learningassistant.auth;

public class InvalidGoogleCredentialException extends RuntimeException {
    public InvalidGoogleCredentialException() {
        super("Google sign-in could not be verified. Please try again.");
    }
}
