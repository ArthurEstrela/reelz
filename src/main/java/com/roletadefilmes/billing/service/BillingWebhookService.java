package com.roletadefilmes.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.billing.api.dto.WebhookResponse;
import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.domain.exception.InvalidBillingWebhookException;
import com.roletadefilmes.billing.domain.exception.InvalidBillingWebhookSignatureException;
import com.roletadefilmes.billing.integration.MercadoPagoProperties;
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
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingWebhookService {

    private static final BillingProvider PROVIDER = BillingProvider.MERCADO_PAGO;

    private final BillingSubscriptionRepository subscriptionRepository;
    private final PaymentWebhookEventRepository eventRepository;
    private final PaymentGateway paymentGateway;
    private final MercadoPagoProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BillingWebhookService(
            BillingSubscriptionRepository subscriptionRepository,
            PaymentWebhookEventRepository eventRepository,
            PaymentGateway paymentGateway,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.paymentGateway = paymentGateway;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public WebhookResponse handle(byte[] rawBody, String dataId, String signature, String requestId) {
        verifySignature(dataId, signature, requestId);
        var root = parse(rawBody);
        var resourceId = requiredText(root.path("data"), "id");
        if (!constantTimeEquals(resourceId.toLowerCase(Locale.ROOT), dataId.toLowerCase(Locale.ROOT))) {
            throw new InvalidBillingWebhookSignatureException();
        }

        var type = requiredText(root, "type");
        var action = text(root, "action");
        var now = clock.instant();
        var payloadHash = sha256(rawBody);
        var result = fetchResult(type, resourceId, action, payloadHash);

        if (eventRepository.existsByProviderAndProviderEventId(PROVIDER, result.eventId())) {
            return new WebhookResponse(true, true);
        }
        var event = eventRepository.saveAndFlush(new PaymentWebhookEventEntity(
                PROVIDER,
                result.eventId(),
                result.eventType(),
                payloadHash,
                now
        ));
        if (result.processed()) event.processed(now);
        else event.ignored(now);
        return new WebhookResponse(true, false);
    }

    private ProcessingResult fetchResult(String type, String resourceId, String action, String payloadHash) {
        return switch (type) {
            case "subscription_preapproval" -> processSubscription(
                    paymentGateway.getSubscription(resourceId), type, action
            );
            case "subscription_authorized_payment" -> processPayment(
                    paymentGateway.getAuthorizedPayment(resourceId), type, action
            );
            case "payment" -> processPayment(paymentGateway.getPayment(resourceId), type, action);
            default -> new ProcessingResult(
                    limited("notification:" + resourceId + ":" + payloadHash.substring(0, 24), 160),
                    eventType(type, action),
                    false
            );
        };
    }

    private ProcessingResult processSubscription(
            PaymentGateway.ProviderSubscription providerSubscription,
            String type,
            String action
    ) {
        var status = normalized(providerSubscription.status());
        var candidate = resolveSubscription(
                providerSubscription.externalReference(),
                providerSubscription.id()
        );
        var processed = candidate.map(subscription -> switch (status) {
            case "canceled", "cancelled" -> cancel(subscription, clock.instant());
            case "paused" -> markPastDue(subscription);
            default -> false;
        }).orElse(false);
        return new ProcessingResult(
                limited("subscription:" + providerSubscription.id() + ":" + status, 160),
                eventType(type, action),
                processed
        );
    }

    private ProcessingResult processPayment(
            PaymentGateway.ProviderPayment providerPayment,
            String type,
            String action
    ) {
        var status = normalized(providerPayment.status());
        var eventId = limited("payment:" + providerPayment.id() + ":" + status, 160);
        if (eventRepository.existsByProviderAndProviderEventId(PROVIDER, eventId)) {
            return new ProcessingResult(eventId, eventType(type, action), false);
        }
        var candidate = resolveSubscription(
                providerPayment.externalReference(),
                providerPayment.subscriptionId()
        );
        var processed = candidate.map(subscription -> switch (status) {
            case "approved" -> applyApprovedPayment(subscription, providerPayment, clock.instant());
            case "rejected", "cancelled" -> markPastDue(subscription);
            case "refunded", "charged_back" -> cancel(subscription, clock.instant());
            default -> false;
        }).orElse(false);
        return new ProcessingResult(eventId, eventType(type, action), processed);
    }

    private boolean applyApprovedPayment(
            BillingSubscriptionEntity subscription,
            PaymentGateway.ProviderPayment payment,
            Instant now
    ) {
        if (!"BRL".equals(payment.currency()) || payment.amountCents() != subscription.getAmountCents()) {
            throw new InvalidBillingWebhookException(
                    "O valor ou a moeda do pagamento não corresponde ao plano reservado."
            );
        }
        if (subscription.getStatus() == BillingSubscriptionStatus.CHECKOUT_PENDING) {
            var providerSubscriptionId = StringUtils.hasText(payment.subscriptionId())
                    ? payment.subscriptionId()
                    : subscription.getProviderSubscriptionId();
            if (!StringUtils.hasText(providerSubscriptionId)) {
                throw new InvalidBillingWebhookException("Pagamento aprovado sem identificador da assinatura.");
            }
            var periodEnd = subscription.getPlanCode().nextPeriodEnd(now);
            subscription.activate(providerSubscriptionId, payment.paymentMethod(), now, periodEnd);
            subscription.getUser().activatePremium(periodEnd);
            return true;
        }
        if (subscription.getStatus() == BillingSubscriptionStatus.ACTIVE
                || subscription.getStatus() == BillingSubscriptionStatus.PAST_DUE) {
            var periodStart = subscription.getCurrentPeriodEnd() != null
                    && subscription.getCurrentPeriodEnd().isAfter(now)
                    ? subscription.getCurrentPeriodEnd()
                    : now;
            var periodEnd = subscription.getPlanCode().nextPeriodEnd(periodStart);
            subscription.renew(payment.paymentMethod(), periodStart, periodEnd);
            subscription.getUser().activatePremium(periodEnd);
            return true;
        }
        return false;
    }

    private boolean markPastDue(BillingSubscriptionEntity subscription) {
        if (subscription.getStatus() != BillingSubscriptionStatus.ACTIVE) return false;
        subscription.markPastDue();
        return true;
    }

    private boolean cancel(BillingSubscriptionEntity subscription, Instant now) {
        if (subscription.getStatus() == BillingSubscriptionStatus.CANCELED) return false;
        subscription.cancel(now);
        var hasAnotherActive = subscriptionRepository.existsByUserIdAndStatusAndIdNot(
                subscription.getUser().getId(), BillingSubscriptionStatus.ACTIVE, subscription.getId()
        );
        if (!hasAnotherActive) subscription.getUser().deactivatePremium();
        return true;
    }

    private Optional<BillingSubscriptionEntity> resolveSubscription(
            String externalReference,
            String providerSubscriptionId
    ) {
        if (StringUtils.hasText(externalReference)) {
            try {
                var byId = subscriptionRepository.findById(UUID.fromString(externalReference));
                if (byId.isPresent() && byId.orElseThrow().getProvider() == PROVIDER) return byId;
            } catch (IllegalArgumentException ignored) {
                // External references are free-form at the provider; try its subscription ID next.
            }
        }
        if (StringUtils.hasText(providerSubscriptionId)) {
            return subscriptionRepository.findByProviderAndProviderSubscriptionId(
                    PROVIDER, providerSubscriptionId
            );
        }
        return Optional.empty();
    }

    private void verifySignature(String dataId, String signature, String requestId) {
        if (!properties.enabled() || !StringUtils.hasText(properties.webhookSecret())
                || !StringUtils.hasText(dataId) || !StringUtils.hasText(signature)
                || !StringUtils.hasText(requestId)) {
            throw new InvalidBillingWebhookSignatureException();
        }
        var timestamp = signaturePart(signature, "ts");
        var receivedHash = signaturePart(signature, "v1");
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(receivedHash)) {
            throw new InvalidBillingWebhookSignatureException();
        }
        var manifest = "id:" + dataId.toLowerCase(Locale.ROOT)
                + ";request-id:" + requestId
                + ";ts:" + timestamp + ";";
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            ));
            var expected = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            if (!constantTimeEquals(expected, receivedHash.toLowerCase(Locale.ROOT))) {
                throw new InvalidBillingWebhookSignatureException();
            }
        } catch (InvalidBillingWebhookSignatureException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidBillingWebhookException("Não foi possível validar o webhook de pagamento.", exception);
        }
    }

    private String signaturePart(String signature, String name) {
        for (var part : signature.split(",")) {
            var pair = part.trim().split("=", 2);
            if (pair.length == 2 && name.equals(pair[0])) return pair[1];
        }
        return null;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)
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
            throw new InvalidBillingWebhookException(
                    "Webhook de pagamento sem o campo obrigatório " + field + "."
            );
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isValueNode() && !value.isNull() ? value.asText() : null;
    }

    private String normalized(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase(Locale.ROOT) : "unknown";
    }

    private String eventType(String type, String action) {
        return limited(StringUtils.hasText(action) ? type + ":" + action : type, 100);
    }

    private String limited(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String sha256(byte[] rawBody) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawBody));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private record ProcessingResult(String eventId, String eventType, boolean processed) {
    }
}
