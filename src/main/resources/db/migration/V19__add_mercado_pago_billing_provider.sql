ALTER TABLE billing_subscription
    DROP CONSTRAINT ck_billing_subscription_provider;

ALTER TABLE billing_subscription
    ADD CONSTRAINT ck_billing_subscription_provider
        CHECK (provider IN ('ABACATEPAY', 'MERCADO_PAGO'));

ALTER TABLE payment_webhook_event
    DROP CONSTRAINT ck_payment_webhook_event_provider;

ALTER TABLE payment_webhook_event
    ADD CONSTRAINT ck_payment_webhook_event_provider
        CHECK (provider IN ('ABACATEPAY', 'MERCADO_PAGO'));

-- Checkouts antigos não podem mais ser concluídos sem o gateway removido.
UPDATE billing_subscription
SET status = 'CANCELED',
    checkout_url = NULL,
    canceled_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'ABACATEPAY'
  AND status = 'CHECKOUT_PENDING';
