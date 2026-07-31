package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncRunEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncCheckpointRepository;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TmdbCatalogProgressService {

    private static final String SOURCE = "TMDB";

    private final CatalogSyncRunRepository runRepository;
    private final CatalogSyncCheckpointRepository checkpointRepository;

    public TmdbCatalogProgressService(
            CatalogSyncRunRepository runRepository,
            CatalogSyncCheckpointRepository checkpointRepository
    ) {
        this.runRepository = runRepository;
        this.checkpointRepository = checkpointRepository;
    }

    @Transactional
    public CatalogSyncRunEntity startRun(String region, CatalogSyncTrigger trigger, Instant startedAt) {
        return runRepository.save(new CatalogSyncRunEntity(SOURCE, region, trigger, startedAt));
    }

    @Transactional
    public void finishRun(CatalogSyncRunEntity run, TmdbCatalogSyncResult result, Instant completedAt) {
        run.complete(
                result.status(),
                result.providersRequested(),
                result.providersCompleted(),
                result.providersFailed(),
                result.pagesRead(),
                result.moviesDiscovered(),
                result.moviesImported(),
                result.offersImported(),
                result.moviesSkipped(),
                result.errorMessage(),
                completedAt
        );
        runRepository.save(run);
    }

    @Transactional
    public CatalogSyncCheckpointEntity getOrCreateCheckpoint(String region, int providerId) {
        return checkpointRepository.findBySourceAndRegionAndExternalProviderId(SOURCE, region, providerId)
                .orElseGet(() -> checkpointRepository.save(
                        new CatalogSyncCheckpointEntity(SOURCE, region, providerId)
                ));
    }

    @Transactional
    public CatalogSyncCheckpointEntity recordPageSuccess(
            CatalogSyncCheckpointEntity checkpoint,
            int page,
            int nextPage,
            Instant syncedAt
    ) {
        checkpoint.recordSuccess(page, nextPage, syncedAt);
        return checkpointRepository.save(checkpoint);
    }

    @Transactional
    public void recordFailure(CatalogSyncCheckpointEntity checkpoint, String error) {
        checkpoint.recordFailure(error);
        checkpointRepository.save(checkpoint);
    }
}
