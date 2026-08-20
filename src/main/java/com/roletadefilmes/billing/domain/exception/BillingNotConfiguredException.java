package com.roletadefilmes.billing.domain.exception;

public class BillingNotConfiguredException extends RuntimeException {
    public BillingNotConfiguredException() {
        super("Os pagamentos ainda não estão disponíveis. Tente novamente mais tarde.");
    }
}
