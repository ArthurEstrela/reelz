CREATE TABLE user_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    premium_until TIMESTAMPTZ,
    timezone VARCHAR(50) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    onboarding_completed_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_account_plan CHECK (plan IN ('FREE', 'PREMIUM')),
    CONSTRAINT ck_user_account_email_normalized CHECK (email = LOWER(email)),
    CONSTRAINT ck_user_account_country_code CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE UNIQUE INDEX uk_user_account_email ON user_account (LOWER(email));

CREATE TABLE movie_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tmdb_id BIGINT NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    overview TEXT,
    poster_path VARCHAR(255),
    release_date DATE,
    vote_average NUMERIC(3, 1),
    vote_count INTEGER NOT NULL DEFAULT 0,
    genre_ids INTEGER[] NOT NULL DEFAULT '{}',
    adult BOOLEAN NOT NULL DEFAULT FALSE,
    original_language VARCHAR(10),
    runtime_minutes INTEGER,
    tmdb_last_synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movie_cache_vote_average CHECK (vote_average IS NULL OR vote_average BETWEEN 0 AND 10),
    CONSTRAINT ck_movie_cache_vote_count CHECK (vote_count >= 0),
    CONSTRAINT ck_movie_cache_runtime CHECK (runtime_minutes IS NULL OR runtime_minutes > 0)
);

CREATE INDEX idx_movie_cache_genre_ids ON movie_cache USING GIN (genre_ids);

CREATE TABLE streaming_provider (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tmdb_provider_id INTEGER NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    logo_path VARCHAR(255),
    display_priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_streaming_provider_priority CHECK (display_priority >= 0)
);

CREATE TABLE vibe (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(80) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    genre_ids INTEGER[] NOT NULL DEFAULT '{}',
    query_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    rules_version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_vibe_rules_version CHECK (rules_version > 0)
);

CREATE INDEX idx_vibe_genre_ids ON vibe USING GIN (genre_ids);

CREATE TABLE user_movie_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    movie_id UUID NOT NULL REFERENCES movie_cache(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    watched_at TIMESTAMPTZ,
    user_rating SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_movie_history_user_movie UNIQUE (user_id, movie_id),
    CONSTRAINT ck_user_movie_history_status CHECK (status IN ('WATCHED', 'WATCHLIST')),
    CONSTRAINT ck_user_movie_history_rating CHECK (user_rating IS NULL OR user_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_user_movie_history_watchlist_metadata CHECK (
        status <> 'WATCHLIST' OR (watched_at IS NULL AND user_rating IS NULL)
    )
);

-- CURRENT_TIMESTAMP is intentionally enforced by a trigger rather than a CHECK:
-- a time-dependent CHECK is not an immutable database invariant in PostgreSQL.
CREATE OR REPLACE FUNCTION reject_future_watched_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.watched_at IS NOT NULL AND NEW.watched_at > CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'watched_at cannot be in the future'
            USING ERRCODE = '23514', CONSTRAINT = 'ck_user_movie_history_watched_at_not_future';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_user_movie_history_watched_at_not_future
    BEFORE INSERT OR UPDATE OF watched_at ON user_movie_history
    FOR EACH ROW EXECUTE FUNCTION reject_future_watched_at();

CREATE INDEX idx_user_movie_history_user_status_updated
    ON user_movie_history (user_id, status, updated_at DESC);
CREATE INDEX idx_user_movie_history_movie ON user_movie_history (movie_id);

CREATE TABLE user_streaming_preference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES streaming_provider(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_streaming_preference UNIQUE (user_id, provider_id)
);

CREATE INDEX idx_user_streaming_preference_provider
    ON user_streaming_preference (provider_id, user_id);

CREATE TABLE movie_streaming_offer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL REFERENCES movie_cache(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES streaming_provider(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    monetization_type VARCHAR(20) NOT NULL,
    attribution_url VARCHAR(1000),
    available_from TIMESTAMPTZ,
    available_until TIMESTAMPTZ,
    last_synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_movie_streaming_offer UNIQUE (movie_id, provider_id, country_code, monetization_type),
    CONSTRAINT ck_movie_streaming_offer_country_code CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_movie_streaming_offer_type CHECK (monetization_type IN ('FLATRATE', 'FREE', 'ADS', 'RENT', 'BUY')),
    CONSTRAINT ck_movie_streaming_offer_window CHECK (
        available_from IS NULL OR available_until IS NULL OR available_until > available_from
    )
);

CREATE INDEX idx_movie_streaming_offer_lookup
    ON movie_streaming_offer (provider_id, country_code, monetization_type, movie_id);

CREATE TABLE roulette_daily_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    base_spins_used INTEGER NOT NULL DEFAULT 0,
    rewarded_spins_granted INTEGER NOT NULL DEFAULT 0,
    rewarded_spins_used INTEGER NOT NULL DEFAULT 0,
    timezone_snapshot VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roulette_daily_usage_user_date UNIQUE (user_id, usage_date),
    CONSTRAINT ck_roulette_daily_usage_nonnegative CHECK (
        base_spins_used >= 0 AND rewarded_spins_granted >= 0 AND rewarded_spins_used >= 0
    ),
    CONSTRAINT ck_roulette_daily_usage_reward_balance CHECK (rewarded_spins_used <= rewarded_spins_granted)
);

CREATE TABLE roulette_spin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100) NOT NULL,
    filters JSONB NOT NULL DEFAULT '{}'::jsonb,
    movie_id UUID REFERENCES movie_cache(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uk_roulette_spin_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_roulette_spin_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'NO_CANDIDATE', 'FAILED')),
    CONSTRAINT ck_roulette_spin_result CHECK (status <> 'SUCCEEDED' OR movie_id IS NOT NULL)
);

CREATE INDEX idx_roulette_spin_user_created ON roulette_spin (user_id, created_at DESC);

CREATE TABLE reward_grant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    ad_provider VARCHAR(80) NOT NULL,
    external_reward_id VARCHAR(255) NOT NULL,
    amount INTEGER NOT NULL DEFAULT 3,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reward_grant_external UNIQUE (ad_provider, external_reward_id),
    CONSTRAINT ck_reward_grant_amount CHECK (amount > 0)
);

CREATE INDEX idx_reward_grant_user_granted ON reward_grant (user_id, granted_at DESC);

CREATE TABLE user_legal_acceptance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    document_type VARCHAR(40) NOT NULL,
    document_version VARCHAR(40) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    evidence JSONB NOT NULL DEFAULT '{}'::jsonb,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_legal_acceptance UNIQUE (user_id, document_type, document_version),
    CONSTRAINT ck_user_legal_acceptance_document_type CHECK (document_type IN ('TERMS_OF_USE', 'PRIVACY_POLICY')),
    CONSTRAINT ck_user_legal_acceptance_country_code CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_user_legal_acceptance_user_accepted
    ON user_legal_acceptance (user_id, accepted_at DESC);
