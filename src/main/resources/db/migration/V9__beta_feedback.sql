CREATE TABLE beta_feedback (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    score SMALLINT NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_beta_feedback_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT ck_beta_feedback_message CHECK (message IS NULL OR length(btrim(message)) > 0)
);

CREATE INDEX idx_beta_feedback_created_at
    ON beta_feedback (created_at DESC);
