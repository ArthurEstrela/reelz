package com.roletadefilmes.catalog.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

@Repository
public class CatalogSyncLeaseRepository {

    private final JdbcTemplate jdbcTemplate;

    public CatalogSyncLeaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquire(String jobName, UUID owner, Duration duration) {
        int updated = jdbcTemplate.update("""
                UPDATE catalog_sync_lease
                   SET lease_owner = ?,
                       lease_until = CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
                       updated_at = CURRENT_TIMESTAMP
                 WHERE job_name = ?
                   AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP OR lease_owner = ?)
                """, owner, duration.toSeconds(), jobName, owner);
        return updated == 1;
    }

    public void release(String jobName, UUID owner) {
        jdbcTemplate.update("""
                UPDATE catalog_sync_lease
                   SET lease_owner = NULL,
                       lease_until = NULL,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE job_name = ? AND lease_owner = ?
                """, jobName, owner);
    }
}
