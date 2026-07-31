package com.roletadefilmes.auth.service;

import com.roletadefilmes.auth.api.dto.LoginRequest;
import com.roletadefilmes.auth.domain.exception.InvalidCredentialsException;
import com.roletadefilmes.security.JwtService;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserAccountEntity user;

    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("reelz-dummy-password")).thenReturn("dummy-bcrypt-hash");
        service = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void shouldReturnATokenForValidCredentials() {
        var userId = UUID.randomUUID();
        var request = new LoginRequest(" Person@Reelz.App ", "correct-password");
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("person@reelz.app"))
                .thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(user.getId()).thenReturn(userId);
        when(user.getOnboardingCompletedAt()).thenReturn(Instant.now());
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken(userId)).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(7_200L);

        var response = service.login(request);

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.onboardingCompleted()).isTrue();
    }

    @Test
    void shouldReturnTheSameGenericErrorForAnUnknownEmail() {
        var request = new LoginRequest("unknown@reelz.app", "some-password");
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("unknown@reelz.app"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(jwtService, never()).generateToken(org.mockito.ArgumentMatchers.any());
        verify(passwordEncoder).matches("some-password", "dummy-bcrypt-hash");
    }

    @Test
    void shouldReturnTheSameGenericErrorForAWrongPassword() {
        var request = new LoginRequest("person@reelz.app", "wrong-password");
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("person@reelz.app"))
                .thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(jwtService, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }
}
