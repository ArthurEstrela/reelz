package com.roletadefilmes.account.service;

import com.roletadefilmes.account.domain.AccountActionTokenType;
import com.roletadefilmes.account.domain.exception.InvalidAccountTokenException;
import com.roletadefilmes.account.persistence.entity.AccountActionTokenEntity;
import com.roletadefilmes.account.persistence.repository.AccountActionTokenRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {

    @Mock AccountActionTokenRepository tokenRepository;
    @Mock UserAccountRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock UserAccountEntity user;

    private AccountSecurityService service;

    @BeforeEach
    void setUp() {
        service = new AccountSecurityService(
                tokenRepository,
                userRepository,
                passwordEncoder,
                eventPublisher,
                Clock.fixed(Instant.parse("2026-08-14T12:00:00Z"), ZoneOffset.UTC),
                Duration.ofHours(24),
                Duration.ofMinutes(30),
                Duration.ofMinutes(1)
        );
    }

    @Test
    void shouldNotRevealWhetherPasswordResetEmailExists() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("unknown@reelz.app"))
                .thenReturn(Optional.empty());

        service.requestPasswordReset(" Unknown@Reelz.App ");

        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldPersistOnlyTheTokenHashAndPublishTheRawTokenAfterIssuing() {
        var userId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("person@reelz.app"))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("person@reelz.app");
        when(user.getDisplayName()).thenReturn("Pessoa");

        service.requestPasswordReset("person@reelz.app");

        var event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        var mailEvent = (AccountMailEvent) event.getValue();
        assertThat(mailEvent.rawToken()).hasSizeGreaterThanOrEqualTo(40);
        assertThat(mailEvent.tokenType()).isEqualTo(AccountActionTokenType.PASSWORD_RESET);
        var persisted = ArgumentCaptor.forClass(AccountActionTokenEntity.class);
        verify(tokenRepository).save(persisted.capture());
        assertThat(persisted.getValue().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(mailEvent.rawToken());
    }

    @Test
    void shouldRejectUnknownOrExpiredTokensWithTheSameError() {
        when(tokenRepository.findForConsumption(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmVerification("invalid"))
                .isInstanceOf(InvalidAccountTokenException.class);
    }
}
