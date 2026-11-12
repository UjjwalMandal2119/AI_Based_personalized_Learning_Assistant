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

@Service
public class AuthService {
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(StudentRepository students, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TokenService tokenService) {
        this.students = students;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
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

    private AuthDtos.AuthResponse response(Student student) {
        return new AuthDtos.AuthResponse(tokenService.createToken(student), "Bearer",
                student.getDisplayName(), student.getEmail());
    }
}
