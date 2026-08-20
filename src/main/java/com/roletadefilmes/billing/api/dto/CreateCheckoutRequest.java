package com.roletadefilmes.billing.api.dto;

import com.roletadefilmes.billing.domain.BillingPlanCode;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutRequest(@NotNull BillingPlanCode planCode) {
}
