ALTER TABLE user_account
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE user_account
    ADD CONSTRAINT ck_user_account_auth_version CHECK (auth_version >= 0);
