package com.roletadefilmes.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.billing.domain.BillingPlanCode;
import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.domain.exception.InvalidBillingWebhookSignatureException;
import com.roletadefilmes.billing.integration.MercadoPagoProperties;
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
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingWebhookServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T20:00:00Z");
    private static final String SECRET = "mercado-pago-webhook-secret";
    private static final String REQUEST_ID = "request-123";
    private static final String TIMESTAMP = "1787169600";

    @Mock
    private BillingSubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentWebhookEventRepository eventRepository;
    @Mock
    private PaymentGateway paymentGateway;

    private BillingWebhookService service;

    @BeforeEach
    void setUp() {
        var properties = new MercadoPagoProperties(
                true,
                "TEST-access-token",
                SECRET,
                1290,
                9990,
                "https://cinegiro.app",
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );
        service = new BillingWebhookService(
                subscriptionRepository,
                eventRepository,
                paymentGateway,
                properties,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldActivatePremiumOnlyAfterAnApprovedProviderPayment() throws Exception {
        var user = new UserAccountEntity(
                "webhook@cinegiro.app", "hash", "Webhook User", "America/Sao_Paulo", "BR"
        );
        var subscription = new BillingSubscriptionEntity(
                user, BillingProvider.MERCADO_PAGO, BillingPlanCode.PREMIUM_MONTHLY, 1290
        );
        subscription.attachCheckout(
                "preapproval_123", "preapproval_123", "https://pay.example/preapproval_123"
        );
        var payload = """
                {"id":98765,"type":"subscription_authorized_payment",
                 "action":"subscription_authorized_payment.updated","data":{"id":"456"}}
                """.getBytes(StandardCharsets.UTF_8);
        when(paymentGateway.getAuthorizedPayment("456")).thenReturn(new PaymentGateway.ProviderPayment(
                "789", "preapproval_123", null, "approved", "BRL", 1290, "visa"
        ));
        when(eventRepository.existsByProviderAndProviderEventId(
                BillingProvider.MERCADO_PAGO, "payment:789:approved"
        )).thenReturn(false);
        when(eventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByProviderAndProviderSubscriptionId(
                BillingProvider.MERCADO_PAGO, "preapproval_123"
        )).thenReturn(Optional.of(subscription));

        var response = service.handle(payload, "456", sign("456"), REQUEST_ID);

        assertThat(response.accepted()).isTrue();
        assertThat(subscription.getStatus()).isEqualTo(BillingSubscriptionStatus.ACTIVE);
        assertThat(subscription.getProviderSubscriptionId()).isEqualTo("preapproval_123");
        assertThat(subscription.getPaymentMethod()).isEqualTo("visa");
        assertThat(user.isPremiumAt(NOW)).isTrue();
        assertThat(subscription.getCurrentPeriodEnd()).isAfter(NOW);
    }

    @Test
    void shouldAcknowledgeTheSameApprovedPaymentOnlyOnce() throws Exception {
        var payload = """
                {"id":98766,"type":"payment","action":"payment.updated","data":{"id":"789"}}
                """.getBytes(StandardCharsets.UTF_8);
        when(paymentGateway.getPayment("789")).thenReturn(new PaymentGateway.ProviderPayment(
                "789", "preapproval_123", null, "approved", "BRL", 1290, "visa"
        ));
        when(eventRepository.existsByProviderAndProviderEventId(
                BillingProvider.MERCADO_PAGO, "payment:789:approved"
        )).thenReturn(true);

        var response = service.handle(payload, "789", sign("789"), REQUEST_ID);

        assertThat(response.duplicate()).isTrue();
    }

    @Test
    void shouldRejectAnInvalidMercadoPagoSignature() {
        var payload = """
                {"id":98767,"type":"payment","action":"payment.created","data":{"id":"790"}}
                """.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.handle(
                payload,
                "790",
                "ts=" + TIMESTAMP + ",v1=invalid",
                REQUEST_ID
        )).isInstanceOf(InvalidBillingWebhookSignatureException.class);
    }

    private String sign(String dataId) throws Exception {
        var manifest = "id:" + dataId.toLowerCase() + ";request-id:" + REQUEST_ID + ";ts:" + TIMESTAMP + ";";
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var hash = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        return "ts=" + TIMESTAMP + ",v1=" + hash;
    }
}
