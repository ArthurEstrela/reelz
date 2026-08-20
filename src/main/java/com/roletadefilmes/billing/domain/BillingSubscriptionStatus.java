package com.roletadefilmes.billing.domain;

public enum BillingSubscriptionStatus {
    CHECKOUT_PENDING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    EXPIRED
}
