package com.learningassistant.auth;

public interface GoogleIdentityVerifier {
    GoogleIdentity verify(String idToken);

    record GoogleIdentity(String subject, String email, String name, boolean emailVerified) {}
}
