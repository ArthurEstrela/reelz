package com.roletadefilmes.billing.api.dto;

import com.roletadefilmes.billing.domain.BillingPlanCode;

import java.util.List;

public record BillingPlanResponse(
        BillingPlanCode code,
        String name,
        int priceCents,
        String currency,
        String interval,
        boolean available,
        boolean recommended,
        List<String> features
) {
}
