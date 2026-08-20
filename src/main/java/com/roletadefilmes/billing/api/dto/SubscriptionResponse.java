package com.roletadefilmes.billing.api.dto;

import com.roletadefilmes.billing.domain.BillingPlanCode;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.user.domain.PlanType;

import java.time.Instant;

public record SubscriptionResponse(
        PlanType accountPlan,
        boolean premium,
        BillingPlanCode planCode,
        BillingSubscriptionStatus status,
        int amountCents,
        String currency,
        Instant currentPeriodEnd,
        Instant canceledAt,
        String checkoutUrl,
        boolean cancelable
) {
}
