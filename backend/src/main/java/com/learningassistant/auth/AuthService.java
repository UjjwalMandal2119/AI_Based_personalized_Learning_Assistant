package com.learningassistant.auth;

import com.learningassistant.common.ConflictException;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final GoogleIdentityVerifier googleIdentityVerifier;

    public AuthService(StudentRepository students, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TokenService tokenService,
                       GoogleIdentityVerifier googleIdentityVerifier) {
        this.students = students;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.googleIdentityVerifier = googleIdentityVerifier;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (students.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Student student = students.save(new Student(email, passwordEncoder.encode(request.password()),
                request.displayName().trim()));
        return response(student);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(Locale.ROOT), request.password()));
        Student student = students.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated student was not found"));
        return response(student);
    }

    @Transactional
    public AuthDtos.AuthResponse loginWithGoogle(AuthDtos.GoogleAuthRequest request) {
        GoogleIdentityVerifier.GoogleIdentity identity = verifiedGoogleIdentity(request.idToken());
        String email = identity.email().trim().toLowerCase(Locale.ROOT);
        return students.findByGoogleSubject(identity.subject())
                .map(this::response)
                .orElseGet(() -> {
                    if (students.existsByEmail(email)) {
                        throw new ConflictException(
                                "This email already has an account. Sign in with your password, then link Google from your dashboard.");
                    }
                    String displayName = normalizeDisplayName(identity.name(), email);
                    Student student = new Student(email,
                            passwordEncoder.encode(UUID.randomUUID().toString()), displayName);
                    student.linkGoogleSubject(identity.subject());
                    return response(students.save(student));
                });
    }

    @Transactional
    public AuthDtos.AuthResponse linkGoogle(AuthDtos.GoogleAuthRequest request, String authenticatedEmail) {
        GoogleIdentityVerifier.GoogleIdentity identity = verifiedGoogleIdentity(request.idToken());
        String email = identity.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equals(authenticatedEmail.trim().toLowerCase(Locale.ROOT))) {
            throw new ConflictException("Sign in to the account that uses this Google email before linking.");
        }

        Student student = students.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated student was not found"));
        if (student.getGoogleSubject() != null && !student.getGoogleSubject().equals(identity.subject())) {
            throw new ConflictException("A different Google account is already linked.");
        }
        students.findByGoogleSubject(identity.subject()).ifPresent(linked -> {
            if (!linked.getId().equals(student.getId())) {
                throw new ConflictException("This Google account is already linked to another account.");
            }
        });
        student.linkGoogleSubject(identity.subject());
        return response(students.save(student));
    }

    private GoogleIdentityVerifier.GoogleIdentity verifiedGoogleIdentity(String idToken) {
        GoogleIdentityVerifier.GoogleIdentity identity = googleIdentityVerifier.verify(idToken);
        if (!identity.emailVerified()) {
            throw new InvalidGoogleCredentialException();
        }
        return identity;
    }

    private AuthDtos.AuthResponse response(Student student) {
        return new AuthDtos.AuthResponse(tokenService.createToken(student), "Bearer",
                student.getDisplayName(), student.getEmail(), student.getGoogleSubject() != null);
    }

    private String normalizeDisplayName(String name, String email) {
        String displayName = name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name.trim();
        return displayName.length() <= 80 ? displayName : displayName.substring(0, 80);
    }
}
