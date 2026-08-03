package com.roletadefilmes.user.service;

import com.roletadefilmes.admin.config.AdminProperties;
import com.roletadefilmes.legal.persistence.entity.UserLegalAcceptanceEntity;
import com.roletadefilmes.legal.persistence.repository.UserLegalAcceptanceRepository;
import com.roletadefilmes.observability.ReelzMetrics;
import com.roletadefilmes.user.api.dto.RegisterUserRequest;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private UserLegalAcceptanceRepository legalAcceptanceRepository;

    @Mock
    private ReelzMetrics metrics;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(
                userRepository,
                legalAcceptanceRepository,
                new BCryptPasswordEncoder(4),
                "terms-1.0",
                "privacy-1.0",
                metrics,
                new AdminProperties()
        );
    }

    @Test
    void shouldHashThePasswordAndPersistBothLegalAcceptances() {
        var request = new RegisterUserRequest(
                "Pessoa",
                "Person@Reelz.App",
                "plain-password",
                "America/Sao_Paulo",
                "BR",
                true
        );
        when(userRepository.existsByEmailIgnoreCase("person@reelz.app")).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.register(request);

        var userCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(new BCryptPasswordEncoder().matches(
                "plain-password",
                userCaptor.getValue().getPasswordHash()
        )).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserLegalAcceptanceEntity>> acceptanceCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(legalAcceptanceRepository).saveAll(acceptanceCaptor.capture());
        assertThat(acceptanceCaptor.getValue()).hasSize(2);
    }
}
