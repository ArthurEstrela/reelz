package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;
import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityClient;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityMovieData;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityProperties;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityProviderData;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncRunEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncLeaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StreamingAvailabilityCatalogSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingAvailabilityCatalogSyncService.class);
    private static final String LEASE_NAME = "STREAMING_AVAILABILITY_CATALOG";
    private static final Duration MAX_CHANGES_LOOKBACK = Duration.ofDays(30);

    private final StreamingAvailabilityClient client;
    private final StreamingAvailabilityCatalogPersistenceService persistenceService;
    private final StreamingAvailabilityCatalogProgressService progressService;
    private final CatalogSyncLeaseRepository leaseRepository;
    private final StreamingAvailabilityProperties properties;
    private final Clock clock;

    public StreamingAvailabilityCatalogSyncService(
            StreamingAvailabilityClient client,
            StreamingAvailabilityCatalogPersistenceService persistenceService,
            StreamingAvailabilityCatalogProgressService progressService,
            CatalogSyncLeaseRepository leaseRepository,
            StreamingAvailabilityProperties properties,
            Clock clock
    ) {
        this.client = client;
        this.persistenceService = persistenceService;
        this.progressService = progressService;
        this.leaseRepository = leaseRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public StreamingAvailabilitySyncResult synchronize() {
        return synchronize(CatalogSyncTrigger.MANUAL);
    }

    public StreamingAvailabilitySyncResult synchronize(CatalogSyncTrigger trigger) {
        UUID leaseOwner = UUID.randomUUID();
        String region = properties.country().toUpperCase(Locale.ROOT);
        if (!leaseRepository.tryAcquire(LEASE_NAME, leaseOwner, properties.leaseDuration())) {
            LOGGER.info("Streaming Availability synchronization skipped because another instance owns the lease");
            return StreamingAvailabilitySyncResult.skipped("Another catalog synchronization is already running");
        }

        CatalogSyncRunEntity run = null;
        CatalogSyncCheckpointEntity checkpoint = null;
        try {
            Instant startedAt = now();
            run = progressService.startRun(region, trigger, startedAt);
            var selectedProviders = selectConfiguredProviders(client.listProviders());
            if (selectedProviders.isEmpty()) {
                throw new IllegalStateException("No configured Streaming Availability providers are available in " + region);
            }
            var selectedCatalogs = selectAvailableCatalogs(selectedProviders);
            persistenceService.synchronizeProviders(
                    selectedProviders,
                    properties.deactivateUnconfiguredProviders()
            );

            checkpoint = progressService.getOrCreateCheckpoint(region);
            Set<String> allowedServiceIds = selectedProviders.stream()
                    .map(StreamingAvailabilityProviderData::serviceId)
                    .collect(Collectors.toUnmodifiableSet());
            var metrics = new SyncMetrics(selectedProviders.size());

            if (checkpoint.isBootstrapCompleted()) {
                synchronizeChanges(checkpoint, selectedCatalogs, allowedServiceIds, region, metrics);
            } else {
                synchronizeBootstrap(checkpoint, selectedCatalogs, allowedServiceIds, region, metrics);
            }

            var result = metrics.toResult();
            progressService.finishRun(run, result, now());
            logResult(result, checkpoint.isBootstrapCompleted());
            return result;
        } catch (RuntimeException exception) {
            if (checkpoint != null) {
                progressService.recordFailure(checkpoint, safeMessage(exception));
            }
            var result = failedResult(exception);
            if (run != null) {
                progressService.finishRun(run, result, now());
            }
            LOGGER.error("Streaming Availability catalog synchronization failed", exception);
            return result;
        } finally {
            leaseRepository.release(LEASE_NAME, leaseOwner);
        }
    }

    private void synchronizeBootstrap(
            CatalogSyncCheckpointEntity checkpoint,
            List<String> catalogs,
            Set<String> allowedServiceIds,
            String region,
            SyncMetrics metrics
    ) {
        int maximumPages = Math.max(1, properties.bootstrapPagesPerRun());
        int pageNumber = checkpoint.getNextPage();
        String cursor = checkpoint.getNextCursor();

        for (int pageOffset = 0; pageOffset < maximumPages; pageOffset++) {
            var page = client.searchMovies(catalogs, cursor);
            importMovies(page.movies(), allowedServiceIds, region, metrics);
            metrics.pagesRead++;

            if (!page.hasMore()) {
                progressService.completeBootstrap(checkpoint, pageNumber, now());
                return;
            }
            requireNextCursor(page.nextCursor());
            checkpoint = progressService.recordBootstrapPage(
                    checkpoint,
                    pageNumber,
                    page.nextCursor(),
                    now()
            );
            cursor = page.nextCursor();
            pageNumber++;
        }
    }

    private void synchronizeChanges(
            CatalogSyncCheckpointEntity checkpoint,
            List<String> catalogs,
            Set<String> allowedServiceIds,
            String region,
            SyncMetrics metrics
    ) {
        Instant current = now();
        Instant windowFrom = checkpoint.getSyncWindowFrom();
        Instant windowTo = checkpoint.getSyncWindowTo();
        if (windowFrom == null || windowTo == null) {
            Instant lastSuccess = checkpoint.getLastSuccessAt() == null
                    ? checkpoint.getBootstrapCompletedAt()
                    : checkpoint.getLastSuccessAt();
            windowFrom = laterOf(lastSuccess, current.minus(MAX_CHANGES_LOOKBACK));
            windowTo = current;
        }
        if (!windowFrom.isBefore(windowTo)) {
            return;
        }

        int maximumPages = Math.max(1, properties.changesPagesPerRun());
        int pageNumber = checkpoint.getNextPage();
        String cursor = checkpoint.getNextCursor();
        for (int pageOffset = 0; pageOffset < maximumPages; pageOffset++) {
            var page = client.listUpdatedMovies(catalogs, windowFrom, windowTo, cursor);
            importMovies(page.movies(), allowedServiceIds, region, metrics);
            metrics.pagesRead++;

            if (!page.hasMore()) {
                progressService.completeIncrementalSync(checkpoint, pageNumber, windowTo);
                return;
            }
            requireNextCursor(page.nextCursor());
            checkpoint = progressService.recordIncrementalPage(
                    checkpoint,
                    pageNumber,
                    page.nextCursor(),
                    windowFrom,
                    windowTo,
                    now()
            );
            cursor = page.nextCursor();
            pageNumber++;
        }
    }

    private void importMovies(
            List<StreamingAvailabilityMovieData> movies,
            Set<String> allowedServiceIds,
            String region,
            SyncMetrics metrics
    ) {
        metrics.moviesDiscovered += movies.size();
        for (var movie : movies) {
            int offers = persistenceService.upsertMovie(movie, allowedServiceIds, region, now());
            metrics.offersImported += offers;
            if (offers == 0) {
                metrics.moviesSkipped++;
            } else {
                metrics.moviesImported++;
            }
        }
    }

    private List<StreamingAvailabilityProviderData> selectConfiguredProviders(
            List<StreamingAvailabilityProviderData> availableProviders
    ) {
        Set<String> configuredServiceIds = properties.catalogs().stream()
                .map(this::serviceIdFromCatalog)
                .collect(Collectors.toUnmodifiableSet());
        return availableProviders.stream()
                .filter(provider -> configuredServiceIds.contains(provider.serviceId()))
                .toList();
    }

    private List<String> selectAvailableCatalogs(List<StreamingAvailabilityProviderData> selectedProviders) {
        Set<String> availableServiceIds = selectedProviders.stream()
                .map(StreamingAvailabilityProviderData::serviceId)
                .map(serviceId -> serviceId.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        List<String> selectedCatalogs = properties.catalogs().stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(catalog -> availableServiceIds.contains(serviceIdFromCatalog(catalog)))
                .distinct()
                .toList();
        List<String> ignoredCatalogs = properties.catalogs().stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(catalog -> !availableServiceIds.contains(serviceIdFromCatalog(catalog)))
                .distinct()
                .toList();
        if (!ignoredCatalogs.isEmpty()) {
            LOGGER.warn(
                    "Ignoring Streaming Availability catalogs unavailable in {}: {}",
                    properties.country().toUpperCase(Locale.ROOT),
                    ignoredCatalogs
            );
        }
        if (selectedCatalogs.isEmpty()) {
            throw new IllegalStateException(
                    "No configured Streaming Availability catalogs are available in "
                            + properties.country().toUpperCase(Locale.ROOT)
            );
        }
        return selectedCatalogs;
    }

    private String serviceIdFromCatalog(String catalog) {
        int separator = catalog.indexOf('.');
        return (separator < 0 ? catalog : catalog.substring(0, separator)).toLowerCase(Locale.ROOT);
    }

    private void requireNextCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            throw new IllegalStateException("Streaming Availability returned hasMore=true without nextCursor");
        }
    }

    private Instant laterOf(Instant first, Instant second) {
        if (first == null) {
            return second;
        }
        return first.isAfter(second) ? first : second;
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private StreamingAvailabilitySyncResult failedResult(RuntimeException exception) {
        return new StreamingAvailabilitySyncResult(
                CatalogSyncStatus.FAILED,
                0, 0, 0, 0, 0, 0, 0, 0,
                safeMessage(exception)
        );
    }

    private void logResult(StreamingAvailabilitySyncResult result, boolean bootstrapCompleted) {
        LOGGER.info(
                "Streaming Availability catalog synchronized: status={}, bootstrapCompleted={}, providers={}/{}, "
                        + "pages={}, discovered={}, imported={}, offers={}, skipped={}",
                result.status(),
                bootstrapCompleted,
                result.providersCompleted(),
                result.providersRequested(),
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

    private static final class SyncMetrics {
        private final int providersRequested;
        private int pagesRead;
        private int moviesDiscovered;
        private int moviesImported;
        private int offersImported;
        private int moviesSkipped;

        private SyncMetrics(int providersRequested) {
            this.providersRequested = providersRequested;
        }

        private StreamingAvailabilitySyncResult toResult() {
            return new StreamingAvailabilitySyncResult(
                    CatalogSyncStatus.SUCCEEDED,
                    providersRequested,
                    providersRequested,
                    0,
                    pagesRead,
                    moviesDiscovered,
                    moviesImported,
                    offersImported,
                    moviesSkipped,
                    null
            );
        }
    }
}
