package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "reelz.tmdb.sync-on-startup", havingValue = "true")
public class TmdbCatalogStartupSync implements ApplicationRunner {

    private final TmdbCatalogSyncService syncService;

    public TmdbCatalogStartupSync(TmdbCatalogSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncService.synchronize(CatalogSyncTrigger.STARTUP);
    }
}
