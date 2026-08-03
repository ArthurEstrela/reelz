ALTER TABLE user_account
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE user_account
    ADD CONSTRAINT ck_user_account_role CHECK (role IN ('USER', 'ADMIN'));

CREATE TABLE product_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    session_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_event_user_event UNIQUE (user_id, event_id),
    CONSTRAINT ck_product_event_type CHECK (event_type IN (
        'HOME_VIEWED',
        'WATCH_PROVIDER_CLICKED',
        'COUPLE_MODE_INTERESTED',
        'GROUP_MODE_INTERESTED'
    )),
    CONSTRAINT ck_product_event_properties_object CHECK (jsonb_typeof(properties) = 'object')
);

CREATE INDEX idx_product_event_occurred_at
    ON product_event (occurred_at DESC);

CREATE INDEX idx_product_event_type_occurred
    ON product_event (event_type, occurred_at DESC);

CREATE INDEX idx_product_event_user_occurred
    ON product_event (user_id, occurred_at DESC);

CREATE INDEX idx_product_event_session
    ON product_event (session_id, occurred_at);
