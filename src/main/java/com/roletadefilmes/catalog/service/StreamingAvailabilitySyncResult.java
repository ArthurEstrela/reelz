package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;

public record StreamingAvailabilitySyncResult(
        CatalogSyncStatus status,
        int providersRequested,
        int providersCompleted,
        int providersFailed,
        int pagesRead,
        int moviesDiscovered,
        int moviesImported,
        int offersImported,
        int moviesSkipped,
        String errorMessage
) {
    public static StreamingAvailabilitySyncResult skipped(String reason) {
        return new StreamingAvailabilitySyncResult(
                CatalogSyncStatus.SKIPPED,
                0, 0, 0, 0, 0, 0, 0, 0,
                reason
        );
    }
}
