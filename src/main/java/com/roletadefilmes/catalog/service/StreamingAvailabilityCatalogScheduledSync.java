package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "reelz.streaming-availability",
        name = {"enabled", "scheduled-sync-enabled"},
        havingValue = "true"
)
public class StreamingAvailabilityCatalogScheduledSync {

    private final StreamingAvailabilityCatalogSyncService syncService;

    public StreamingAvailabilityCatalogScheduledSync(StreamingAvailabilityCatalogSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(
            cron = "${reelz.streaming-availability.sync-cron:0 0 4 * * *}",
            zone = "${reelz.streaming-availability.sync-zone:UTC}"
    )
    public void synchronizeCatalog() {
        syncService.synchronize(CatalogSyncTrigger.SCHEDULED);
    }
}
