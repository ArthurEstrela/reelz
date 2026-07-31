CREATE TABLE catalog_sync_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(30) NOT NULL,
    region VARCHAR(2) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    providers_requested INTEGER NOT NULL DEFAULT 0,
    providers_completed INTEGER NOT NULL DEFAULT 0,
    providers_failed INTEGER NOT NULL DEFAULT 0,
    pages_read INTEGER NOT NULL DEFAULT 0,
    movies_discovered INTEGER NOT NULL DEFAULT 0,
    movies_imported INTEGER NOT NULL DEFAULT 0,
    offers_imported INTEGER NOT NULL DEFAULT 0,
    movies_skipped INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_catalog_sync_run_region CHECK (region ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_catalog_sync_run_trigger CHECK (trigger_type IN ('STARTUP', 'SCHEDULED', 'MANUAL')),
    CONSTRAINT ck_catalog_sync_run_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_catalog_sync_run_counters CHECK (
        providers_requested >= 0 AND providers_completed >= 0 AND providers_failed >= 0
        AND pages_read >= 0 AND movies_discovered >= 0 AND movies_imported >= 0
        AND offers_imported >= 0 AND movies_skipped >= 0
    )
);

CREATE INDEX idx_catalog_sync_run_source_started
    ON catalog_sync_run (source, region, started_at DESC);

CREATE TABLE catalog_sync_checkpoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(30) NOT NULL,
    region VARCHAR(2) NOT NULL,
    external_provider_id INTEGER NOT NULL,
    next_page INTEGER NOT NULL DEFAULT 1,
    last_successful_page INTEGER,
    last_success_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_catalog_sync_checkpoint UNIQUE (source, region, external_provider_id),
    CONSTRAINT ck_catalog_sync_checkpoint_region CHECK (region ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_catalog_sync_checkpoint_provider CHECK (external_provider_id > 0),
    CONSTRAINT ck_catalog_sync_checkpoint_page CHECK (next_page BETWEEN 1 AND 500),
    CONSTRAINT ck_catalog_sync_checkpoint_last_page CHECK (
        last_successful_page IS NULL OR last_successful_page BETWEEN 1 AND 500
    )
);

CREATE TABLE catalog_sync_lease (
    job_name VARCHAR(80) PRIMARY KEY,
    lease_owner UUID,
    lease_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO catalog_sync_lease (job_name) VALUES ('TMDB_CATALOG');
