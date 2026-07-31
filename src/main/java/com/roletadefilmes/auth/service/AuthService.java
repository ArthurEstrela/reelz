package com.roletadefilmes.auth.service;

import com.roletadefilmes.auth.api.dto.LoginRequest;
import com.roletadefilmes.auth.api.dto.LoginResponse;
import com.roletadefilmes.auth.domain.exception.InvalidCredentialsException;
import com.roletadefilmes.security.JwtService;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyPasswordHash;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode("reelz-dummy-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        var normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        var userCandidate = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail);
        var passwordHash = userCandidate
                .map(user -> user.getPasswordHash())
                .orElse(dummyPasswordHash);

        var credentialsMatch = passwordEncoder.matches(request.password(), passwordHash);
        if (userCandidate.isEmpty() || !credentialsMatch) {
            throw new InvalidCredentialsException();
        }
        var user = userCandidate.orElseThrow();

        return new LoginResponse(
                jwtService.generateToken(user.getId()),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getOnboardingCompletedAt() != null
        );
    }
}
