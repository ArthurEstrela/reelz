CREATE TABLE billing_subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    provider VARCHAR(30) NOT NULL,
    provider_checkout_id VARCHAR(120),
    provider_subscription_id VARCHAR(120),
    checkout_url VARCHAR(2048),
    plan_code VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount_cents INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    payment_method VARCHAR(30),
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_billing_subscription_checkout UNIQUE (provider, provider_checkout_id),
    CONSTRAINT uk_billing_subscription_provider_id UNIQUE (provider, provider_subscription_id),
    CONSTRAINT ck_billing_subscription_provider CHECK (provider IN ('ABACATEPAY')),
    CONSTRAINT ck_billing_subscription_plan CHECK (plan_code IN ('PREMIUM_MONTHLY', 'PREMIUM_ANNUAL')),
    CONSTRAINT ck_billing_subscription_status CHECK (
        status IN ('CHECKOUT_PENDING', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'EXPIRED')
    ),
    CONSTRAINT ck_billing_subscription_amount CHECK (amount_cents > 0),
    CONSTRAINT ck_billing_subscription_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_billing_subscription_period CHECK (
        current_period_start IS NULL OR current_period_end IS NULL OR current_period_end > current_period_start
    )
);

CREATE UNIQUE INDEX uk_billing_subscription_user_live
    ON billing_subscription (user_id)
    WHERE status IN ('CHECKOUT_PENDING', 'ACTIVE', 'PAST_DUE');

CREATE INDEX idx_billing_subscription_user_created
    ON billing_subscription (user_id, created_at DESC);

CREATE TABLE payment_webhook_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(160) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_sha256 VARCHAR(64) NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_webhook_event UNIQUE (provider, provider_event_id),
    CONSTRAINT ck_payment_webhook_event_provider CHECK (provider IN ('ABACATEPAY')),
    CONSTRAINT ck_payment_webhook_event_status CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'IGNORED')),
    CONSTRAINT ck_payment_webhook_event_hash CHECK (payload_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_payment_webhook_event_received
    ON payment_webhook_event (provider, received_at DESC);
