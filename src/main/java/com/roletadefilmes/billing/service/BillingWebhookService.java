package com.roletadefilmes.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.billing.api.dto.WebhookResponse;
import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.domain.exception.InvalidBillingWebhookException;
import com.roletadefilmes.billing.domain.exception.InvalidBillingWebhookSignatureException;
import com.roletadefilmes.billing.integration.AbacatePayProperties;
import com.roletadefilmes.billing.persistence.entity.BillingSubscriptionEntity;
import com.roletadefilmes.billing.persistence.entity.PaymentWebhookEventEntity;
import com.roletadefilmes.billing.persistence.repository.BillingSubscriptionRepository;
import com.roletadefilmes.billing.persistence.repository.PaymentWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingWebhookService {

    private final BillingSubscriptionRepository subscriptionRepository;
    private final PaymentWebhookEventRepository eventRepository;
    private final AbacatePayProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BillingWebhookService(
            BillingSubscriptionRepository subscriptionRepository,
            PaymentWebhookEventRepository eventRepository,
            AbacatePayProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public WebhookResponse handle(byte[] rawBody, String urlSecret, String signature) {
        verifySignature(rawBody, urlSecret, signature);
        var root = parse(rawBody);
        var eventId = requiredText(root, "id");
        var eventType = requiredText(root, "event");
        if (eventRepository.existsByProviderAndProviderEventId(BillingProvider.ABACATEPAY, eventId)) {
            return new WebhookResponse(true, true);
        }

        var now = clock.instant();
        var event = eventRepository.saveAndFlush(new PaymentWebhookEventEntity(
                eventId, eventType, sha256(rawBody), now
        ));
        if (root.path("devMode").asBoolean(false) && !properties.acceptDevEvents()) {
            event.ignored(now);
            return new WebhookResponse(true, false);
        }

        var processed = switch (eventType) {
            case "subscription.completed" -> activate(root.path("data"), now);
            case "subscription.renewed" -> renew(root.path("data"), now);
            case "subscription.payment_failed" -> markPastDue(root.path("data"));
            case "subscription.cancelled" -> cancel(root.path("data"), now);
            default -> false;
        };
        if (processed) {
            event.processed(now);
        } else {
            event.ignored(now);
        }
        return new WebhookResponse(true, false);
    }

    private boolean activate(JsonNode data, Instant now) {
        var candidate = resolveSubscription(data);
        if (candidate.isEmpty()) return false;
        var subscription = candidate.orElseThrow();
        if (subscription.getStatus() != BillingSubscriptionStatus.CHECKOUT_PENDING) return false;

        var checkoutAmount = data.path("checkout").path("amount").asInt(-1);
        if (checkoutAmount != subscription.getAmountCents()) {
            throw new InvalidBillingWebhookException("O valor pago não corresponde ao plano reservado.");
        }

        var providerSubscriptionId = text(data.path("subscription"), "id");
        if (!StringUtils.hasText(providerSubscriptionId)) {
            throw new InvalidBillingWebhookException("Webhook de assinatura sem identificador da assinatura.");
        }
        var end = subscription.getPlanCode().nextPeriodEnd(now);
        subscription.activate(providerSubscriptionId, paymentMethod(data), now, end);
        subscription.getUser().activatePremium(end);
        return true;
    }

    private boolean renew(JsonNode data, Instant now) {
        var candidate = resolveSubscription(data);
        if (candidate.isEmpty()) return false;
        var subscription = candidate.orElseThrow();
        if (subscription.getStatus() != BillingSubscriptionStatus.ACTIVE
                && subscription.getStatus() != BillingSubscriptionStatus.PAST_DUE) {
            return false;
        }
        var periodStart = subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(now)
                ? subscription.getCurrentPeriodEnd()
                : now;
        var periodEnd = subscription.getPlanCode().nextPeriodEnd(periodStart);
        subscription.renew(paymentMethod(data), periodStart, periodEnd);
        subscription.getUser().activatePremium(periodEnd);
        return true;
    }

    private boolean markPastDue(JsonNode data) {
        var candidate = resolveSubscription(data);
        if (candidate.isEmpty()) return false;
        var subscription = candidate.orElseThrow();
        if (subscription.getStatus() != BillingSubscriptionStatus.ACTIVE) return false;
        subscription.markPastDue();
        return true;
    }

    private boolean cancel(JsonNode data, Instant now) {
        var candidate = resolveSubscription(data);
        if (candidate.isEmpty()) return false;
        var subscription = candidate.orElseThrow();
        if (subscription.getStatus() == BillingSubscriptionStatus.CANCELED) return false;
        subscription.cancel(now);
        var hasAnotherActive = subscriptionRepository.existsByUserIdAndStatusAndIdNot(
                subscription.getUser().getId(), BillingSubscriptionStatus.ACTIVE, subscription.getId()
        );
        if (!hasAnotherActive) subscription.getUser().deactivatePremium();
        return true;
    }

    private Optional<BillingSubscriptionEntity> resolveSubscription(JsonNode data) {
        var checkout = data.path("checkout");
        var externalId = text(checkout, "externalId");
        if (StringUtils.hasText(externalId)) {
            try {
                var byId = subscriptionRepository.findById(UUID.fromString(externalId));
                if (byId.isPresent()) return byId;
            } catch (IllegalArgumentException ignored) {
                // The provider also accepts arbitrary external IDs; continue with provider identifiers.
            }
        }
        var checkoutId = text(checkout, "id");
        if (StringUtils.hasText(checkoutId)) {
            var byCheckout = subscriptionRepository.findByProviderAndProviderCheckoutId(
                    BillingProvider.ABACATEPAY, checkoutId
            );
            if (byCheckout.isPresent()) return byCheckout;
        }
        var subscriptionId = text(data.path("subscription"), "id");
        if (StringUtils.hasText(subscriptionId)) {
            return subscriptionRepository.findByProviderAndProviderSubscriptionId(
                    BillingProvider.ABACATEPAY, subscriptionId
            );
        }
        return Optional.empty();
    }

    private String paymentMethod(JsonNode data) {
        var value = text(data.path("subscription"), "method");
        return StringUtils.hasText(value) ? value : null;
    }

    private void verifySignature(byte[] rawBody, String urlSecret, String signature) {
        if (!properties.enabled()
                || !constantTimeEquals(properties.webhookSecret(), urlSecret)
                || !StringUtils.hasText(properties.webhookHmacKey())
                || !StringUtils.hasText(signature)) {
            throw new InvalidBillingWebhookSignatureException();
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.webhookHmacKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var expected = mac.doFinal(rawBody);
            var received = Base64.getDecoder().decode(signature);
            if (!MessageDigest.isEqual(expected, received)) {
                throw new InvalidBillingWebhookSignatureException();
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidBillingWebhookSignatureException();
        } catch (Exception exception) {
            throw new InvalidBillingWebhookException("Não foi possível validar o webhook de pagamento.", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private JsonNode parse(byte[] rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new InvalidBillingWebhookException("Payload do webhook de pagamento inválido.", exception);
        }
    }

    private String requiredText(JsonNode node, String field) {
        var value = text(node, field);
        if (!StringUtils.hasText(value)) {
            throw new InvalidBillingWebhookException("Webhook de pagamento sem o campo obrigatório " + field + ".");
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private String sha256(byte[] rawBody) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawBody));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
