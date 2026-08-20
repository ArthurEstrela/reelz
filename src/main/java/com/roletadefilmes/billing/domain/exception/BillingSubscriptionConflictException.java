package com.roletadefilmes.billing.domain.exception;

public class BillingSubscriptionConflictException extends RuntimeException {
    public BillingSubscriptionConflictException(String message) {
        super(message);
    }
}
