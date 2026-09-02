package com.roletadefilmes.billing.service;

import com.roletadefilmes.billing.domain.BillingPlanCode;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.domain.exception.BillingSubscriptionConflictException;
import com.roletadefilmes.billing.integration.AbacatePayProperties;
import com.roletadefilmes.billing.persistence.entity.BillingSubscriptionEntity;
import com.roletadefilmes.billing.persistence.repository.BillingSubscriptionRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T20:00:00Z");

    @Mock
    private BillingSubscriptionRepository subscriptionRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private PaymentGateway paymentGateway;

    private BillingService service;

    @BeforeEach
    void setUp() {
        var properties = new AbacatePayProperties(
                true,
                "abc_dev_key",
                "webhook-secret",
                "hmac-key",
                "prod_monthly",
                "prod_annual",
                1290,
                9990,
                "https://cinegiro.app",
                List.of("CARD"),
                true,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );
        service = new BillingService(
                subscriptionRepository,
                userRepository,
                paymentGateway,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateCheckoutWithAnInternalIdAndProviderMetadata() {
        var userId = UUID.randomUUID();
        var subscriptionId = UUID.randomUUID();
        var user = newUser();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            BillingSubscriptionEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", subscriptionId);
            return entity;
        });
        when(paymentGateway.createSubscriptionCheckout(any())).thenReturn(
                new PaymentGateway.CheckoutResult("bill_123", "https://pay.example/bill_123", 1290)
        );

        var response = service.createCheckout(userId, BillingPlanCode.PREMIUM_MONTHLY);

        assertThat(response.checkoutUrl()).isEqualTo("https://pay.example/bill_123");
        assertThat(response.reused()).isFalse();
        var command = org.mockito.ArgumentCaptor.forClass(PaymentGateway.CheckoutCommand.class);
        verify(paymentGateway).createSubscriptionCheckout(command.capture());
        assertThat(command.getValue().externalId()).isEqualTo(subscriptionId.toString());
        assertThat(command.getValue().productId()).isEqualTo("prod_monthly");
        assertThat(command.getValue().metadata()).containsEntry("reelzUserId", userId.toString());
    }

    @Test
    void shouldReuseThePendingCheckoutWithoutCreatingASecondCharge() {
        var userId = UUID.randomUUID();
        var user = newUser();
        var pending = new BillingSubscriptionEntity(user, BillingPlanCode.PREMIUM_MONTHLY, 1290);
        pending.attachCheckout("bill_existing", "https://pay.example/existing");
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(pending));

        var response = service.createCheckout(userId, BillingPlanCode.PREMIUM_MONTHLY);

        assertThat(response.reused()).isTrue();
        assertThat(response.checkoutUrl()).endsWith("/existing");
        verify(paymentGateway, never()).createSubscriptionCheckout(any());
    }

    @Test
    void shouldRejectAnotherCheckoutForAnAlreadyPremiumAccount() {
        var userId = UUID.randomUUID();
        var user = newUser();
        user.activatePremium(NOW.plusSeconds(3_600));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createCheckout(userId, BillingPlanCode.PREMIUM_ANNUAL))
                .isInstanceOf(BillingSubscriptionConflictException.class);

        verify(paymentGateway, never()).createSubscriptionCheckout(any());
    }

    @Test
    void shouldCancelAtTheProviderBeforeRemovingPremiumAccess() {
        var userId = UUID.randomUUID();
        var user = newUser();
        user.activatePremium(NOW.plusSeconds(3_600));
        var subscription = new BillingSubscriptionEntity(user, BillingPlanCode.PREMIUM_MONTHLY, 1290);
        subscription.activate("subs_123", "CARD", NOW.minusSeconds(60), NOW.plusSeconds(3_600));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                userId,
                List.of(BillingSubscriptionStatus.ACTIVE, BillingSubscriptionStatus.PAST_DUE)
        )).thenReturn(Optional.of(subscription));

        var response = service.cancel(userId);

        verify(paymentGateway).cancelSubscription("subs_123");
        assertThat(response.premium()).isFalse();
        assertThat(subscription.getStatus()).isEqualTo(BillingSubscriptionStatus.CANCELED);
        assertThat(user.isPremiumAt(NOW)).isFalse();
    }

    private UserAccountEntity newUser() {
        return new UserAccountEntity(
                "billing@cinegiro.app",
                "hash",
                "Billing User",
                "America/Sao_Paulo",
                "BR"
        );
    }
}
