package com.roletadefilmes.account.service;

import com.roletadefilmes.account.domain.AccountActionTokenType;
import com.roletadefilmes.account.domain.exception.InvalidAccountTokenException;
import com.roletadefilmes.account.persistence.entity.AccountActionTokenEntity;
import com.roletadefilmes.account.persistence.repository.AccountActionTokenRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AccountSecurityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountActionTokenRepository tokenRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration verificationTtl;
    private final Duration resetTtl;
    private final Duration requestCooldown;

    public AccountSecurityService(
            AccountActionTokenRepository tokenRepository,
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            @Value("${reelz.account.verification-ttl}") Duration verificationTtl,
            @Value("${reelz.account.password-reset-ttl}") Duration resetTtl,
            @Value("${reelz.account.request-cooldown}") Duration requestCooldown
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.verificationTtl = verificationTtl;
        this.resetTtl = resetTtl;
        this.requestCooldown = requestCooldown;
    }

    @Transactional
    public void issueVerification(UserAccountEntity user) {
        if (user.getEmailVerifiedAt() == null) {
            issue(user, AccountActionTokenType.EMAIL_VERIFICATION, verificationTtl);
        }
    }

    @Transactional
    public void requestVerification(String email) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalize(email))
                .filter(user -> user.getEmailVerifiedAt() == null)
                .ifPresent(this::issueVerification);
    }

    @Transactional
    public void confirmVerification(String rawToken) {
        var token = validToken(rawToken, AccountActionTokenType.EMAIL_VERIFICATION);
        var now = clock.instant();
        token.getUser().markEmailVerified(now);
        token.consume(now);
        tokenRepository.consumeOpenTokens(
                token.getUser().getId(),
                AccountActionTokenType.EMAIL_VERIFICATION,
                now
        );
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalize(email))
                .ifPresent(user -> issue(user, AccountActionTokenType.PASSWORD_RESET, resetTtl));
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        var token = validToken(rawToken, AccountActionTokenType.PASSWORD_RESET);
        var now = clock.instant();
        token.getUser().changePassword(passwordEncoder.encode(newPassword));
        token.consume(now);
        tokenRepository.consumeOpenTokens(
                token.getUser().getId(),
                AccountActionTokenType.PASSWORD_RESET,
                now
        );
    }

    private void issue(UserAccountEntity user, AccountActionTokenType type, Duration ttl) {
        var now = clock.instant();
        if (tokenRepository.existsByUserIdAndTokenTypeAndConsumedAtIsNullAndCreatedAtAfter(
                user.getId(), type, now.minus(requestCooldown))) {
            return;
        }
        tokenRepository.consumeOpenTokens(user.getId(), type, now);
        var rawToken = generateToken();
        tokenRepository.save(new AccountActionTokenEntity(user, type, hash(rawToken), now.plus(ttl)));
        eventPublisher.publishEvent(new AccountMailEvent(user.getEmail(), user.getDisplayName(), rawToken, type));
    }

    private AccountActionTokenEntity validToken(String rawToken, AccountActionTokenType type) {
        var now = clock.instant();
        return tokenRepository.findForConsumption(hash(rawToken), type)
                .filter(token -> token.canBeConsumedAt(now))
                .orElseThrow(InvalidAccountTokenException::new);
    }

    private static String generateToken() {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
