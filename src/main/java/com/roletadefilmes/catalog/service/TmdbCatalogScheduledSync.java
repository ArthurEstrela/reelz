package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "reelz.tmdb.scheduled-sync-enabled", havingValue = "true")
public class TmdbCatalogScheduledSync {

    private final TmdbCatalogSyncService syncService;

    public TmdbCatalogScheduledSync(TmdbCatalogSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(
            cron = "${reelz.tmdb.sync-cron:0 0 4 * * *}",
            zone = "${reelz.tmdb.sync-zone:UTC}"
    )
    public void synchronizeCatalog() {
        syncService.synchronize(CatalogSyncTrigger.SCHEDULED);
    }
}
