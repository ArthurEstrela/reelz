package com.roletadefilmes.billing.domain.exception;

public class InvalidBillingWebhookSignatureException extends RuntimeException {
    public InvalidBillingWebhookSignatureException() {
        super("Assinatura do webhook de pagamento inválida.");
    }
}
