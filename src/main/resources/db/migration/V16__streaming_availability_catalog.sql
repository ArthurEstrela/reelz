ALTER TABLE movie_cache
    RENAME COLUMN tmdb_last_synced_at TO metadata_last_synced_at;

ALTER TABLE movie_cache
    ADD COLUMN metadata_source VARCHAR(30) NOT NULL DEFAULT 'TMDB',
    ADD COLUMN external_catalog_id VARCHAR(100),
    ADD COLUMN imdb_id VARCHAR(20);

UPDATE movie_cache
   SET external_catalog_id = tmdb_id::text
 WHERE external_catalog_id IS NULL;

ALTER TABLE movie_cache
    ALTER COLUMN poster_path TYPE VARCHAR(1000);

CREATE UNIQUE INDEX uk_movie_cache_source_external_id
    ON movie_cache (metadata_source, external_catalog_id)
    WHERE external_catalog_id IS NOT NULL;

ALTER TABLE streaming_provider
    ALTER COLUMN tmdb_provider_id DROP NOT NULL,
    ALTER COLUMN logo_path TYPE VARCHAR(1000),
    ADD COLUMN streaming_availability_service_id VARCHAR(80);

CREATE UNIQUE INDEX uk_streaming_provider_streaming_availability_service
    ON streaming_provider (streaming_availability_service_id)
    WHERE streaming_availability_service_id IS NOT NULL;

ALTER TABLE movie_streaming_offer
    ADD COLUMN catalog_source VARCHAR(30) NOT NULL DEFAULT 'TMDB';

ALTER TABLE catalog_sync_checkpoint
    DROP CONSTRAINT ck_catalog_sync_checkpoint_page,
    DROP CONSTRAINT ck_catalog_sync_checkpoint_last_page,
    ADD COLUMN next_cursor TEXT,
    ADD COLUMN sync_window_from TIMESTAMPTZ,
    ADD COLUMN sync_window_to TIMESTAMPTZ,
    ADD COLUMN bootstrap_completed_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_catalog_sync_checkpoint_page CHECK (next_page > 0),
    ADD CONSTRAINT ck_catalog_sync_checkpoint_last_page CHECK (
        last_successful_page IS NULL OR last_successful_page > 0
    ),
    ADD CONSTRAINT ck_catalog_sync_checkpoint_window CHECK (
        (sync_window_from IS NULL AND sync_window_to IS NULL)
        OR (
            sync_window_from IS NOT NULL
            AND sync_window_to IS NOT NULL
            AND sync_window_to >= sync_window_from
        )
    );

INSERT INTO catalog_sync_lease (job_name)
VALUES ('STREAMING_AVAILABILITY_CATALOG')
ON CONFLICT (job_name) DO NOTHING;
