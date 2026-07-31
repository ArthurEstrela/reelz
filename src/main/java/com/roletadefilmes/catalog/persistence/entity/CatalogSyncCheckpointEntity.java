package com.roletadefilmes.catalog.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(
        name = "catalog_sync_checkpoint",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_catalog_sync_checkpoint",
                columnNames = {"source", "region", "external_provider_id"}
        )
)
public class CatalogSyncCheckpointEntity extends AuditableUuidEntity {

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "region", nullable = false, length = 2)
    private String region;

    @Column(name = "external_provider_id", nullable = false)
    private int externalProviderId;

    @Column(name = "next_page", nullable = false)
    private int nextPage = 1;

    @Column(name = "last_successful_page")
    private Integer lastSuccessfulPage;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CatalogSyncCheckpointEntity() {
    }

    public CatalogSyncCheckpointEntity(String source, String region, int externalProviderId) {
        this.source = source;
        this.region = region;
        this.externalProviderId = externalProviderId;
    }

    public int getNextPage() {
        return nextPage;
    }

    public Integer getLastSuccessfulPage() {
        return lastSuccessfulPage;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public String getLastError() {
        return lastError;
    }

    public long getVersion() {
        return version;
    }

    public void recordSuccess(int page, int nextPage, Instant syncedAt) {
        this.lastSuccessfulPage = page;
        this.nextPage = nextPage;
        this.lastSuccessAt = syncedAt;
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.lastError = error == null || error.length() <= 1000 ? error : error.substring(0, 1000);
    }
}
