package com.roletadefilmes.user.service;

import com.roletadefilmes.admin.config.AdminProperties;
import com.roletadefilmes.legal.domain.LegalDocumentType;
import com.roletadefilmes.legal.persistence.entity.UserLegalAcceptanceEntity;
import com.roletadefilmes.legal.persistence.repository.UserLegalAcceptanceRepository;
import com.roletadefilmes.observability.ReelzMetrics;
import com.roletadefilmes.user.api.dto.RegisterUserRequest;
import com.roletadefilmes.user.api.dto.UserResponse;
import com.roletadefilmes.user.domain.exception.EmailAlreadyRegisteredException;
import com.roletadefilmes.user.domain.exception.InvalidTimezoneException;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserRegistrationService {

    private final UserAccountRepository userRepository;
    private final UserLegalAcceptanceRepository legalAcceptanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final String termsVersion;
    private final String privacyVersion;
    private final ReelzMetrics metrics;
    private final AdminProperties adminProperties;

    public UserRegistrationService(
            UserAccountRepository userRepository,
            UserLegalAcceptanceRepository legalAcceptanceRepository,
            PasswordEncoder passwordEncoder,
            @Value("${reelz.legal.terms-version}") String termsVersion,
            @Value("${reelz.legal.privacy-version}") String privacyVersion,
            ReelzMetrics metrics,
            AdminProperties adminProperties
    ) {
        this.userRepository = userRepository;
        this.legalAcceptanceRepository = legalAcceptanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        this.metrics = metrics;
        this.adminProperties = adminProperties;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        validateTimezone(request.timezone());
        var normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        var user = new UserAccountEntity(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.timezone(),
                request.countryCode()
        );
        if (adminProperties.contains(normalizedEmail)) {
            user.promoteToAdmin();
        }

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        legalAcceptanceRepository.saveAll(List.of(
                acceptance(user, LegalDocumentType.TERMS_OF_USE, termsVersion),
                acceptance(user, LegalDocumentType.PRIVACY_POLICY, privacyVersion)
        ));
        metrics.recordUserRegistrationAfterCommit();

        return new UserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPlan(),
                user.getRole(),
                user.getTimezone(),
                user.getCountryCode(),
                user.getOnboardingCompletedAt() != null,
                user.getCreatedAt()
        );
    }

    private UserLegalAcceptanceEntity acceptance(
            UserAccountEntity user,
            LegalDocumentType type,
            String version
    ) {
        return new UserLegalAcceptanceEntity(
                user,
                type,
                version,
                user.getCountryCode(),
                Map.of("source", "registration")
        );
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new InvalidTimezoneException();
        }
    }
}
