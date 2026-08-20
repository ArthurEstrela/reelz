package com.roletadefilmes.billing.service;

import java.util.List;
import java.util.Map;

public interface PaymentGateway {

    CheckoutResult createSubscriptionCheckout(CheckoutCommand command);

    void cancelSubscription(String providerSubscriptionId);

    record CheckoutCommand(
            String externalId,
            String productId,
            String returnUrl,
            String completionUrl,
            List<String> methods,
            Map<String, String> metadata
    ) {
    }

    record CheckoutResult(String providerCheckoutId, String checkoutUrl, int amountCents) {
    }
}
