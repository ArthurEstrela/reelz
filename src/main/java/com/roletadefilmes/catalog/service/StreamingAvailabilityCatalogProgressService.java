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
public class StreamingAvailabilityCatalogProgressService {

    static final String SOURCE = "STREAMING_AVAILABILITY";
    private static final int CATALOG_CHECKPOINT_ID = 1;

    private final CatalogSyncRunRepository runRepository;
    private final CatalogSyncCheckpointRepository checkpointRepository;

    public StreamingAvailabilityCatalogProgressService(
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
    public void finishRun(
            CatalogSyncRunEntity run,
            StreamingAvailabilitySyncResult result,
            Instant completedAt
    ) {
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
    public CatalogSyncCheckpointEntity getOrCreateCheckpoint(String region) {
        return checkpointRepository.findBySourceAndRegionAndExternalProviderId(
                        SOURCE,
                        region,
                        CATALOG_CHECKPOINT_ID
                )
                .orElseGet(() -> checkpointRepository.save(
                        new CatalogSyncCheckpointEntity(SOURCE, region, CATALOG_CHECKPOINT_ID)
                ));
    }

    @Transactional
    public CatalogSyncCheckpointEntity recordBootstrapPage(
            CatalogSyncCheckpointEntity checkpoint,
            int page,
            String nextCursor,
            Instant syncedAt
    ) {
        checkpoint.recordBootstrapPageSuccess(page, nextCursor, syncedAt);
        return checkpointRepository.save(checkpoint);
    }

    @Transactional
    public CatalogSyncCheckpointEntity completeBootstrap(
            CatalogSyncCheckpointEntity checkpoint,
            int page,
            Instant completedAt
    ) {
        checkpoint.completeBootstrap(page, completedAt);
        return checkpointRepository.save(checkpoint);
    }

    @Transactional
    public CatalogSyncCheckpointEntity recordIncrementalPage(
            CatalogSyncCheckpointEntity checkpoint,
            int page,
            String nextCursor,
            Instant windowFrom,
            Instant windowTo,
            Instant syncedAt
    ) {
        checkpoint.recordIncrementalPageSuccess(
                page,
                nextCursor,
                windowFrom,
                windowTo,
                syncedAt
        );
        return checkpointRepository.save(checkpoint);
    }

    @Transactional
    public CatalogSyncCheckpointEntity completeIncrementalSync(
            CatalogSyncCheckpointEntity checkpoint,
            int page,
            Instant completedThrough
    ) {
        checkpoint.completeIncrementalSync(page, completedThrough);
        return checkpointRepository.save(checkpoint);
    }

    @Transactional
    public void recordFailure(CatalogSyncCheckpointEntity checkpoint, String error) {
        checkpoint.recordFailure(error);
        checkpointRepository.save(checkpoint);
    }
}
