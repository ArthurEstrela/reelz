package com.roletadefilmes.billing.domain.exception;

public class BillingSubscriptionNotFoundException extends RuntimeException {
    public BillingSubscriptionNotFoundException() {
        super("Nenhuma assinatura cancelável foi encontrada para esta conta.");
    }
}
