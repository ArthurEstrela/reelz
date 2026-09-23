package com.roletadefilmes.billing.service;

public interface PaymentGateway {

    CheckoutResult createSubscriptionCheckout(CheckoutCommand command);

    void cancelSubscription(String providerSubscriptionId);

    ProviderSubscription getSubscription(String providerSubscriptionId);

    ProviderPayment getAuthorizedPayment(String providerPaymentId);

    ProviderPayment getPayment(String providerPaymentId);

    record CheckoutCommand(
            String externalId,
            String payerEmail,
            String description,
            int amountCents,
            int frequencyInMonths,
            String returnUrl
    ) {
    }

    record CheckoutResult(
            String providerCheckoutId,
            String providerSubscriptionId,
            String checkoutUrl,
            int amountCents
    ) {
    }

    record ProviderSubscription(
            String id,
            String externalReference,
            String status,
            String paymentMethod
    ) {
    }

    record ProviderPayment(
            String id,
            String subscriptionId,
            String externalReference,
            String status,
            String currency,
            int amountCents,
            String paymentMethod
    ) {
    }
}
