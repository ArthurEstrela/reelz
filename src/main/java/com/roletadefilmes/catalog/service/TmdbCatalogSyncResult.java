package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;

public record TmdbCatalogSyncResult(
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
    public static TmdbCatalogSyncResult skipped(String reason) {
        return new TmdbCatalogSyncResult(
                CatalogSyncStatus.SKIPPED,
                0, 0, 0, 0, 0, 0, 0, 0,
                reason
        );
    }
}
