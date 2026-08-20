package com.roletadefilmes.billing.api.dto;

import com.roletadefilmes.billing.domain.BillingPlanCode;

public record CheckoutResponse(
        BillingPlanCode planCode,
        String checkoutUrl,
        boolean reused
) {
}
