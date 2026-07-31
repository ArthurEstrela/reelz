package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;
import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import com.roletadefilmes.catalog.integration.tmdb.TmdbClient;
import com.roletadefilmes.catalog.integration.tmdb.TmdbProperties;
import com.roletadefilmes.catalog.integration.tmdb.TmdbProviderData;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncRunEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncLeaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TmdbCatalogSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbCatalogSyncService.class);
    private static final String LEASE_NAME = "TMDB_CATALOG";
    private static final int MAX_PAGES_PER_PROVIDER = 50;
    private static final int TMDB_MAX_PAGE = 500;

    private final TmdbClient tmdbClient;
    private final TmdbCatalogPersistenceService persistenceService;
    private final TmdbCatalogProgressService progressService;
    private final CatalogSyncLeaseRepository leaseRepository;
    private final TmdbProperties properties;
    private final Clock clock;

    public TmdbCatalogSyncService(
            TmdbClient tmdbClient,
            TmdbCatalogPersistenceService persistenceService,
            TmdbCatalogProgressService progressService,
            CatalogSyncLeaseRepository leaseRepository,
            TmdbProperties properties,
            Clock clock
    ) {
        this.tmdbClient = tmdbClient;
        this.persistenceService = persistenceService;
        this.progressService = progressService;
        this.leaseRepository = leaseRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public TmdbCatalogSyncResult synchronize() {
        return synchronize(CatalogSyncTrigger.MANUAL);
    }

    public TmdbCatalogSyncResult synchronize(CatalogSyncTrigger trigger) {
        UUID leaseOwner = UUID.randomUUID();
        String region = properties.region().toUpperCase();
        if (!leaseRepository.tryAcquire(LEASE_NAME, leaseOwner, properties.leaseDuration())) {
            LOGGER.info("TMDB catalog synchronization skipped because another instance owns the lease");
            return TmdbCatalogSyncResult.skipped("Another catalog synchronization is already running");
        }

        CatalogSyncRunEntity run = null;
        try {
            run = progressService.startRun(region, trigger, Instant.now(clock));
            TmdbCatalogSyncResult result = executeSynchronization(region);
            progressService.finishRun(run, result, Instant.now(clock));
            logResult(result);
            return result;
        } catch (RuntimeException exception) {
            var result = failedResult(exception);
            if (run != null) {
                progressService.finishRun(run, result, Instant.now(clock));
            }
            LOGGER.error("TMDB catalog synchronization failed", exception);
            return result;
        } finally {
            leaseRepository.release(LEASE_NAME, leaseOwner);
        }
    }

    private TmdbCatalogSyncResult executeSynchronization(String region) {
        List<TmdbProviderData> providers = selectProviders(tmdbClient.listMovieProviders());
        if (providers.isEmpty()) {
            throw new IllegalStateException("No TMDB providers were selected for region " + region);
        }

        var metrics = new SyncMetrics(providers.size());
        Set<Long> processedMovieIds = new HashSet<>();
        int pagesPerProvider = Math.max(1, Math.min(properties.pagesPerProvider(), MAX_PAGES_PER_PROVIDER));

        for (TmdbProviderData provider : providers) {
            synchronizeProvider(provider, region, pagesPerProvider, processedMovieIds, metrics);
        }
        persistenceService.activateOnly(
                providers.stream().map(TmdbProviderData::providerId).collect(java.util.stream.Collectors.toSet())
        );
        return metrics.toResult();
    }

    private void synchronizeProvider(
            TmdbProviderData provider,
            String region,
            int pagesPerProvider,
            Set<Long> processedMovieIds,
            SyncMetrics metrics
    ) {
        CatalogSyncCheckpointEntity checkpoint = progressService.getOrCreateCheckpoint(
                region,
                provider.providerId()
        );
        int pageNumber = checkpoint.getNextPage();

        for (int pageOffset = 0; pageOffset < pagesPerProvider; pageOffset++) {
            try {
                var page = tmdbClient.discoverMovies(provider.providerId(), pageNumber);
                metrics.pagesRead++;
                metrics.moviesDiscovered += page.movies().size();

                boolean pageSucceeded = true;
                for (var movie : page.movies()) {
                    if (!processedMovieIds.add(movie.tmdbId())) {
                        metrics.moviesSkipped++;
                        continue;
                    }
                    try {
                        var availability = tmdbClient.findAvailability(movie.tmdbId());
                        if (availability.offers().isEmpty()) {
                            metrics.moviesSkipped++;
                            continue;
                        }
                        metrics.offersImported += persistenceService.upsert(
                                movie,
                                availability,
                                region,
                                Instant.now(clock)
                        );
                        metrics.moviesImported++;
                    } catch (RestClientResponseException exception) {
                        if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                            metrics.moviesSkipped++;
                            continue;
                        }
                        pageSucceeded = false;
                        metrics.addError(provider, exception);
                        break;
                    } catch (RestClientException exception) {
                        pageSucceeded = false;
                        metrics.addError(provider, exception);
                        break;
                    }
                }

                if (!pageSucceeded) {
                    progressService.recordFailure(checkpoint, metrics.lastError());
                    metrics.providersFailed++;
                    return;
                }

                int availablePages = Math.max(1, Math.min(page.totalPages(), TMDB_MAX_PAGE));
                int nextPage = pageNumber >= availablePages ? 1 : pageNumber + 1;
                checkpoint = progressService.recordPageSuccess(
                        checkpoint,
                        pageNumber,
                        nextPage,
                        Instant.now(clock)
                );
                pageNumber = nextPage;
            } catch (RestClientException exception) {
                metrics.addError(provider, exception);
                progressService.recordFailure(checkpoint, metrics.lastError());
                metrics.providersFailed++;
                return;
            }
        }
        metrics.providersCompleted++;
    }

    private List<TmdbProviderData> selectProviders(List<TmdbProviderData> availableProviders) {
        int maximum = Math.max(1, properties.maxProviders());
        if (properties.providerIds().isEmpty()) {
            return availableProviders.stream()
                    .sorted(Comparator.comparingInt(TmdbProviderData::displayPriority))
                    .limit(maximum)
                    .toList();
        }

        Map<Integer, TmdbProviderData> byId = new HashMap<>();
        availableProviders.forEach(provider -> byId.put(provider.providerId(), provider));
        List<TmdbProviderData> selected = new ArrayList<>();
        for (Integer configuredId : properties.providerIds()) {
            TmdbProviderData provider = byId.get(configuredId);
            if (provider == null) {
                LOGGER.warn("Configured TMDB provider {} is not available in region {}", configuredId, properties.region());
                continue;
            }
            selected.add(provider);
            if (selected.size() == maximum) {
                break;
            }
        }
        return List.copyOf(selected);
    }

    private TmdbCatalogSyncResult failedResult(RuntimeException exception) {
        return new TmdbCatalogSyncResult(
                CatalogSyncStatus.FAILED,
                0, 0, 0, 0, 0, 0, 0, 0,
                safeMessage(exception)
        );
    }

    private void logResult(TmdbCatalogSyncResult result) {
        LOGGER.info(
                "TMDB catalog synchronized: status={}, providers={}/{}, failedProviders={}, pages={}, "
                        + "discovered={}, imported={}, offers={}, skipped={}",
                result.status(),
                result.providersCompleted(),
                result.providersRequested(),
                result.providersFailed(),
                result.pagesRead(),
                result.moviesDiscovered(),
                result.moviesImported(),
                result.offersImported(),
                result.moviesSkipped()
        );
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private final class SyncMetrics {
        private final int providersRequested;
        private int providersCompleted;
        private int providersFailed;
        private int pagesRead;
        private int moviesDiscovered;
        private int moviesImported;
        private int offersImported;
        private int moviesSkipped;
        private final List<String> errors = new ArrayList<>();

        private SyncMetrics(int providersRequested) {
            this.providersRequested = providersRequested;
        }

        private void addError(TmdbProviderData provider, Exception exception) {
            String error = "Provider " + provider.providerId() + " (" + provider.name() + "): "
                    + safeMessage(exception);
            errors.add(error);
            LOGGER.warn("Could not synchronize TMDB provider {}: {}", provider.providerId(), safeMessage(exception));
        }

        private String lastError() {
            return errors.isEmpty() ? null : errors.getLast();
        }

        private TmdbCatalogSyncResult toResult() {
            CatalogSyncStatus status = providersFailed == 0
                    ? CatalogSyncStatus.SUCCEEDED
                    : providersCompleted == 0 ? CatalogSyncStatus.FAILED : CatalogSyncStatus.PARTIAL;
            return new TmdbCatalogSyncResult(
                    status,
                    providersRequested,
                    providersCompleted,
                    providersFailed,
                    pagesRead,
                    moviesDiscovered,
                    moviesImported,
                    offersImported,
                    moviesSkipped,
                    errors.isEmpty() ? null : String.join(" | ", errors)
            );
        }
    }
}
