package com.roletadefilmes.catalog.persistence.entity;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;
import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "catalog_sync_run")
public class CatalogSyncRunEntity extends AbstractUuidEntity {

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "region", nullable = false, length = 2)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private CatalogSyncTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CatalogSyncStatus status;

    @Column(name = "providers_requested", nullable = false)
    private int providersRequested;

    @Column(name = "providers_completed", nullable = false)
    private int providersCompleted;

    @Column(name = "providers_failed", nullable = false)
    private int providersFailed;

    @Column(name = "pages_read", nullable = false)
    private int pagesRead;

    @Column(name = "movies_discovered", nullable = false)
    private int moviesDiscovered;

    @Column(name = "movies_imported", nullable = false)
    private int moviesImported;

    @Column(name = "offers_imported", nullable = false)
    private int offersImported;

    @Column(name = "movies_skipped", nullable = false)
    private int moviesSkipped;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected CatalogSyncRunEntity() {
    }

    public CatalogSyncRunEntity(String source, String region, CatalogSyncTrigger trigger, Instant startedAt) {
        this.source = source;
        this.region = region;
        this.trigger = trigger;
        this.status = CatalogSyncStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public CatalogSyncStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void complete(
            CatalogSyncStatus status,
            int providersRequested,
            int providersCompleted,
            int providersFailed,
            int pagesRead,
            int moviesDiscovered,
            int moviesImported,
            int offersImported,
            int moviesSkipped,
            String errorMessage,
            Instant completedAt
    ) {
        this.status = status;
        this.providersRequested = providersRequested;
        this.providersCompleted = providersCompleted;
        this.providersFailed = providersFailed;
        this.pagesRead = pagesRead;
        this.moviesDiscovered = moviesDiscovered;
        this.moviesImported = moviesImported;
        this.offersImported = offersImported;
        this.moviesSkipped = moviesSkipped;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = completedAt;
    }

    private String truncate(String value) {
        return value == null || value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
