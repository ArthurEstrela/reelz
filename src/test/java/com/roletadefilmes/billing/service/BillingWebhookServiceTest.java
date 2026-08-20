package com.roletadefilmes.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.billing.domain.BillingPlanCode;
import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.integration.AbacatePayProperties;
import com.roletadefilmes.billing.persistence.entity.BillingSubscriptionEntity;
import com.roletadefilmes.billing.persistence.repository.BillingSubscriptionRepository;
import com.roletadefilmes.billing.persistence.repository.PaymentWebhookEventRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingWebhookServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T20:00:00Z");
    private static final String SECRET = "webhook-secret";
    private static final String HMAC_KEY = "public-hmac-key";

    @Mock
    private BillingSubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentWebhookEventRepository eventRepository;

    private BillingWebhookService service;

    @BeforeEach
    void setUp() {
        var properties = new AbacatePayProperties(
                true, "api-key", SECRET, HMAC_KEY, "prod_monthly", "prod_annual",
                1290, 9990, "https://reelz.app", List.of("CARD"), true,
                Duration.ofSeconds(5), Duration.ofSeconds(15)
        );
        service = new BillingWebhookService(
                subscriptionRepository,
                eventRepository,
                properties,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldActivatePremiumOnlyAfterAValidCompletedWebhook() throws Exception {
        var user = new UserAccountEntity(
                "webhook@reelz.app", "hash", "Webhook User", "America/Sao_Paulo", "BR"
        );
        var subscription = new BillingSubscriptionEntity(user, BillingPlanCode.PREMIUM_MONTHLY, 1290);
        subscription.attachCheckout("bill_123", "https://pay.example/bill_123");
        var payload = """
                {"id":"log_123","event":"subscription.completed","apiVersion":2,"devMode":false,
                 "data":{"subscription":{"id":"subs_123","method":"CARD"},
                 "checkout":{"id":"bill_123","externalId":null,"amount":1290}}}
                """.getBytes(StandardCharsets.UTF_8);
        when(eventRepository.existsByProviderAndProviderEventId(BillingProvider.ABACATEPAY, "log_123"))
                .thenReturn(false);
        when(eventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByProviderAndProviderCheckoutId(BillingProvider.ABACATEPAY, "bill_123"))
                .thenReturn(Optional.of(subscription));

        var response = service.handle(payload, SECRET, sign(payload));

        assertThat(response.accepted()).isTrue();
        assertThat(subscription.getStatus()).isEqualTo(BillingSubscriptionStatus.ACTIVE);
        assertThat(subscription.getProviderSubscriptionId()).isEqualTo("subs_123");
        assertThat(user.isPremiumAt(NOW)).isTrue();
        assertThat(subscription.getCurrentPeriodEnd()).isAfter(NOW);
    }

    @Test
    void shouldAcknowledgeADuplicateWithoutExtendingTheSubscriptionAgain() throws Exception {
        var payload = "{\"id\":\"log_duplicate\",\"event\":\"subscription.renewed\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(eventRepository.existsByProviderAndProviderEventId(BillingProvider.ABACATEPAY, "log_duplicate"))
                .thenReturn(true);

        var response = service.handle(payload, SECRET, sign(payload));

        assertThat(response.duplicate()).isTrue();
    }

    private String sign(byte[] body) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(body));
    }
}
