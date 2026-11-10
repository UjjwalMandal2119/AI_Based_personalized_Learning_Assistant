package com.learningassistant.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleIdTokenVerifierService implements GoogleIdentityVerifier {
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierService(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(clientId))
                    .build();
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Could not initialize Google ID token verification", exception);
        }
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (clientId.isBlank()) {
            throw new GoogleVerificationUnavailableException("Google sign-in is not configured");
        }
        try {
            GoogleIdToken verifiedToken = verifier.verify(idToken);
            if (verifiedToken == null) {
                throw new InvalidGoogleCredentialException();
            }
            GoogleIdToken.Payload payload = verifiedToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            if (subject == null || subject.isBlank() || email == null || email.isBlank()
                    || !Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new InvalidGoogleCredentialException();
            }
            return new GoogleIdentity(subject, email, name, true);
        } catch (IOException | GeneralSecurityException exception) {
            throw new GoogleVerificationUnavailableException("Google identity verification is temporarily unavailable",
                    exception);
        }
    }
}
