package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;
import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityClient;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityMovieData;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityMoviePage;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityProperties;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityProviderData;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncRunEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncLeaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamingAvailabilityCatalogSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    @Mock
    private StreamingAvailabilityClient client;
    @Mock
    private StreamingAvailabilityCatalogPersistenceService persistenceService;
    @Mock
    private StreamingAvailabilityCatalogProgressService progressService;
    @Mock
    private CatalogSyncLeaseRepository leaseRepository;

    private StreamingAvailabilityCatalogSyncService service;

    @BeforeEach
    void setUp() {
        service = new StreamingAvailabilityCatalogSyncService(
                client,
                persistenceService,
                progressService,
                leaseRepository,
                properties(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCompleteCursorBootstrapAndPersistMoviesLocally() {
        var run = new CatalogSyncRunEntity(
                "STREAMING_AVAILABILITY",
                "BR",
                CatalogSyncTrigger.SCHEDULED,
                NOW
        );
        var checkpoint = new CatalogSyncCheckpointEntity("STREAMING_AVAILABILITY", "BR", 1);
        var movie = movie();

        when(leaseRepository.tryAcquire(eq("STREAMING_AVAILABILITY_CATALOG"), any(), any())).thenReturn(true);
        when(progressService.startRun("BR", CatalogSyncTrigger.SCHEDULED, NOW)).thenReturn(run);
        when(client.listProviders()).thenReturn(providers());
        when(progressService.getOrCreateCheckpoint("BR")).thenReturn(checkpoint);
        when(client.searchMovies(List.of("netflix.subscription", "prime.subscription"), null))
                .thenReturn(new StreamingAvailabilityMoviePage(
                List.of(movie),
                false,
                null
        ));
        when(persistenceService.upsertMovie(movie, Set.of("netflix", "prime"), "BR", NOW))
                .thenReturn(1);

        var result = service.synchronize(CatalogSyncTrigger.SCHEDULED);

        assertThat(result.status()).isEqualTo(CatalogSyncStatus.SUCCEEDED);
        assertThat(result.pagesRead()).isEqualTo(1);
        assertThat(result.moviesImported()).isEqualTo(1);
        verify(persistenceService).synchronizeProviders(providers().subList(0, 2), false);
        verify(progressService).completeBootstrap(checkpoint, 1, NOW);
        verify(progressService).finishRun(run, result, NOW);
        verify(leaseRepository).release(eq("STREAMING_AVAILABILITY_CATALOG"), any());
    }

    @Test
    void shouldUseChangesAfterBootstrapWithoutRestartingFullCatalog() {
        Instant previousSync = NOW.minus(Duration.ofDays(1));
        var run = new CatalogSyncRunEntity(
                "STREAMING_AVAILABILITY",
                "BR",
                CatalogSyncTrigger.SCHEDULED,
                NOW
        );
        var checkpoint = new CatalogSyncCheckpointEntity("STREAMING_AVAILABILITY", "BR", 1);
        checkpoint.completeBootstrap(4, previousSync);

        when(leaseRepository.tryAcquire(eq("STREAMING_AVAILABILITY_CATALOG"), any(), any())).thenReturn(true);
        when(progressService.startRun("BR", CatalogSyncTrigger.SCHEDULED, NOW)).thenReturn(run);
        when(client.listProviders()).thenReturn(providers());
        when(progressService.getOrCreateCheckpoint("BR")).thenReturn(checkpoint);
        when(client.listUpdatedMovies(
                List.of("netflix.subscription", "prime.subscription"),
                previousSync,
                NOW,
                null
        )).thenReturn(
                new StreamingAvailabilityMoviePage(List.of(), false, null)
        );

        var result = service.synchronize(CatalogSyncTrigger.SCHEDULED);

        assertThat(result.status()).isEqualTo(CatalogSyncStatus.SUCCEEDED);
        verify(client, never()).searchMovies(any(), any());
        verify(progressService).completeIncrementalSync(checkpoint, 1, NOW);
    }

    private List<StreamingAvailabilityProviderData> providers() {
        return List.of(
                new StreamingAvailabilityProviderData("netflix", "Netflix", "https://cdn/netflix.png", 0),
                new StreamingAvailabilityProviderData("prime", "Prime Video", "https://cdn/prime.png", 1),
                new StreamingAvailabilityProviderData("hbo", "Max", "https://cdn/max.png", 2)
        );
    }

    private StreamingAvailabilityMovieData movie() {
        return new StreamingAvailabilityMovieData(
                "movie-550",
                550L,
                "tt0137523",
                "Fight Club",
                "Fight Club",
                "Overview",
                "https://cdn/poster.jpg",
                LocalDate.of(1999, 1, 1),
                new BigDecimal("8.4"),
                List.of(18, 53),
                139,
                List.of()
        );
    }

    private StreamingAvailabilityProperties properties() {
        return new StreamingAvailabilityProperties(
                true,
                "test-api-key",
                "https://api.movieofthenight.com/v4",
                "BR",
                "en",
                List.of("netflix.subscription", "prime.subscription", "pluto.free"),
                20,
                10,
                false,
                false,
                false,
                "0 0 4 * * *",
                "America/Sao_Paulo",
                Duration.ofMinutes(30),
                2,
                Duration.ZERO,
                Duration.ofSeconds(5),
                Duration.ofSeconds(20)
        );
    }
}
