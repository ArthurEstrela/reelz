package com.roletadefilmes.billing.domain.exception;

public class InvalidBillingWebhookException extends RuntimeException {
    public InvalidBillingWebhookException(String message) {
        super(message);
    }

    public InvalidBillingWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
