package com.roletadefilmes.billing.service;

import com.roletadefilmes.billing.api.dto.BillingPlanResponse;
import com.roletadefilmes.billing.api.dto.CheckoutResponse;
import com.roletadefilmes.billing.api.dto.SubscriptionResponse;
import com.roletadefilmes.billing.domain.BillingPlanCode;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.domain.exception.BillingNotConfiguredException;
import com.roletadefilmes.billing.domain.exception.BillingProviderException;
import com.roletadefilmes.billing.domain.exception.BillingSubscriptionConflictException;
import com.roletadefilmes.billing.domain.exception.BillingSubscriptionNotFoundException;
import com.roletadefilmes.billing.integration.AbacatePayProperties;
import com.roletadefilmes.billing.persistence.entity.BillingSubscriptionEntity;
import com.roletadefilmes.billing.persistence.repository.BillingSubscriptionRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    private static final List<BillingSubscriptionStatus> LIVE_STATUSES = List.of(
            BillingSubscriptionStatus.CHECKOUT_PENDING,
            BillingSubscriptionStatus.ACTIVE,
            BillingSubscriptionStatus.PAST_DUE
    );
    private static final List<String> PREMIUM_FEATURES = List.of(
            "Giros ilimitados",
            "Combine vários streamings",
            "Crie salas para grupos de até 8 pessoas",
            "Experiência sem anúncios",
            "Apoie a evolução do Reelz"
    );

    private final BillingSubscriptionRepository subscriptionRepository;
    private final UserAccountRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final AbacatePayProperties properties;
    private final Clock clock;

    public BillingService(
            BillingSubscriptionRepository subscriptionRepository,
            UserAccountRepository userRepository,
            PaymentGateway paymentGateway,
            AbacatePayProperties properties,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BillingPlanResponse> plans() {
        return List.of(plan(BillingPlanCode.PREMIUM_MONTHLY, false), plan(BillingPlanCode.PREMIUM_ANNUAL, true));
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse current(UUID userId) {
        var user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(subscription -> response(user.getPlan(), user.isPremiumAt(clock.instant()), subscription))
                .orElseGet(() -> new SubscriptionResponse(
                        user.getPlan(), user.isPremiumAt(clock.instant()), null, null, 0, "BRL",
                        user.getPremiumUntil(), null, null, false
                ));
    }

    @Transactional
    public CheckoutResponse createCheckout(UUID userId, BillingPlanCode planCode) {
        if (!properties.isPlanAvailable(planCode)) {
            throw new BillingNotConfiguredException();
        }
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isPremiumAt(clock.instant())) {
            throw new BillingSubscriptionConflictException("Sua conta já possui o Reelz Premium ativo.");
        }

        var liveSubscription = subscriptionRepository
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(userId, LIVE_STATUSES);
        if (liveSubscription.isPresent()) {
            var existing = liveSubscription.orElseThrow();
            if (existing.getStatus() == BillingSubscriptionStatus.CHECKOUT_PENDING
                    && existing.getPlanCode() == planCode
                    && existing.getCheckoutUrl() != null) {
                return new CheckoutResponse(planCode, existing.getCheckoutUrl(), true);
            }
            throw new BillingSubscriptionConflictException(
                    existing.getStatus() == BillingSubscriptionStatus.CHECKOUT_PENDING
                            ? "Você já possui um checkout de outro plano em andamento."
                            : "Sua conta já possui uma assinatura em andamento."
            );
        }

        var subscription = subscriptionRepository.saveAndFlush(
                new BillingSubscriptionEntity(user, planCode, properties.priceCents(planCode))
        );
        var checkout = paymentGateway.createSubscriptionCheckout(new PaymentGateway.CheckoutCommand(
                subscription.getId().toString(),
                properties.productId(planCode),
                properties.appUrl("/premium"),
                properties.appUrl("/premium?checkout=success"),
                properties.safeMethods(),
                Map.of("reelzUserId", userId.toString(), "reelzPlanCode", planCode.name())
        ));
        if (checkout.amountCents() != subscription.getAmountCents()) {
            throw new BillingProviderException("O valor retornado pelo checkout não corresponde ao plano configurado.");
        }
        subscription.attachCheckout(checkout.providerCheckoutId(), checkout.checkoutUrl());
        return new CheckoutResponse(planCode, checkout.checkoutUrl(), false);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID userId) {
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var subscription = subscriptionRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        userId,
                        List.of(BillingSubscriptionStatus.ACTIVE, BillingSubscriptionStatus.PAST_DUE)
                )
                .orElseThrow(BillingSubscriptionNotFoundException::new);
        if (subscription.getProviderSubscriptionId() == null) {
            throw new BillingSubscriptionNotFoundException();
        }

        paymentGateway.cancelSubscription(subscription.getProviderSubscriptionId());
        subscription.cancel(clock.instant());
        user.deactivatePremium();
        return response(user.getPlan(), false, subscription);
    }

    private BillingPlanResponse plan(BillingPlanCode code, boolean recommended) {
        return new BillingPlanResponse(
                code, code.label(), properties.priceCents(code), "BRL", code.interval(),
                properties.isPlanAvailable(code), recommended, PREMIUM_FEATURES
        );
    }

    private SubscriptionResponse response(
            com.roletadefilmes.user.domain.PlanType accountPlan,
            boolean premium,
            BillingSubscriptionEntity subscription
    ) {
        var cancelable = subscription.getProviderSubscriptionId() != null
                && (subscription.getStatus() == BillingSubscriptionStatus.ACTIVE
                || subscription.getStatus() == BillingSubscriptionStatus.PAST_DUE);
        return new SubscriptionResponse(
                accountPlan, premium, subscription.getPlanCode(), subscription.getStatus(),
                subscription.getAmountCents(), subscription.getCurrency(), subscription.getCurrentPeriodEnd(),
                subscription.getCanceledAt(), subscription.getCheckoutUrl(), cancelable
        );
    }
}
