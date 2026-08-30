package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "reelz.streaming-availability",
        name = {"enabled", "sync-on-startup"},
        havingValue = "true"
)
public class StreamingAvailabilityCatalogStartupSync implements ApplicationRunner {

    private final StreamingAvailabilityCatalogSyncService syncService;

    public StreamingAvailabilityCatalogStartupSync(StreamingAvailabilityCatalogSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncService.synchronize(CatalogSyncTrigger.STARTUP);
    }
}
