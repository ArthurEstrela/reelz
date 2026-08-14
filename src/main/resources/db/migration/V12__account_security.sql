CREATE TABLE account_action_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    token_type VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_account_action_token_type CHECK (
        token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')
    ),
    CONSTRAINT ck_account_action_token_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_account_action_token_consumed CHECK (
        consumed_at IS NULL OR consumed_at >= created_at
    )
);

CREATE INDEX idx_account_action_token_user_type
    ON account_action_token (user_id, token_type, created_at DESC);
CREATE INDEX idx_account_action_token_expiration
    ON account_action_token (expires_at)
    WHERE consumed_at IS NULL;

-- A verificacao passa a ser obrigatoria a partir desta versao. Contas do beta
-- anteriores a este recurso sao consideradas verificadas para evitar lockout.
UPDATE user_account
SET email_verified_at = COALESCE(email_verified_at, created_at)
WHERE deleted_at IS NULL;
