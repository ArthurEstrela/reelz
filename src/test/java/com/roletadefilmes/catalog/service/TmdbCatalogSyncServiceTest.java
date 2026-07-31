package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.domain.CatalogSyncStatus;
import com.roletadefilmes.catalog.domain.CatalogSyncTrigger;
import com.roletadefilmes.catalog.integration.tmdb.TmdbAvailability;
import com.roletadefilmes.catalog.integration.tmdb.TmdbClient;
import com.roletadefilmes.catalog.integration.tmdb.TmdbDiscoverPage;
import com.roletadefilmes.catalog.integration.tmdb.TmdbMovieData;
import com.roletadefilmes.catalog.integration.tmdb.TmdbOfferData;
import com.roletadefilmes.catalog.integration.tmdb.TmdbProperties;
import com.roletadefilmes.catalog.integration.tmdb.TmdbProviderData;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.entity.CatalogSyncRunEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncLeaseRepository;
import com.roletadefilmes.streaming.domain.MonetizationType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbCatalogSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Mock
    private TmdbClient tmdbClient;
    @Mock
    private TmdbCatalogPersistenceService persistenceService;
    @Mock
    private TmdbCatalogProgressService progressService;
    @Mock
    private CatalogSyncLeaseRepository leaseRepository;

    private TmdbCatalogSyncService service;

    @BeforeEach
    void setUp() {
        var properties = new TmdbProperties(
                "token",
                "pt-BR",
                "BR",
                1,
                2,
                List.of(8, 119),
                false,
                false,
                "0 0 4 * * *",
                "America/Sao_Paulo",
                Duration.ofMinutes(30),
                3,
                Duration.ZERO,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );
        service = new TmdbCatalogSyncService(
                tmdbClient,
                persistenceService,
                progressService,
                leaseRepository,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldSynchronizeByProviderAndDeduplicateMoviesWithinTheSameRun() {
        var run = new CatalogSyncRunEntity("TMDB", "BR", CatalogSyncTrigger.SCHEDULED, NOW);
        var netflixCheckpoint = new CatalogSyncCheckpointEntity("TMDB", "BR", 8);
        var primeCheckpoint = new CatalogSyncCheckpointEntity("TMDB", "BR", 119);
        var movie = movie(550L);

        when(leaseRepository.tryAcquire(eq("TMDB_CATALOG"), any(), any())).thenReturn(true);
        when(progressService.startRun("BR", CatalogSyncTrigger.SCHEDULED, NOW)).thenReturn(run);
        when(tmdbClient.listMovieProviders()).thenReturn(List.of(
                new TmdbProviderData(8, "Netflix", "/netflix.jpg", 0),
                new TmdbProviderData(119, "Prime Video", "/prime.jpg", 1)
        ));
        when(progressService.getOrCreateCheckpoint("BR", 8)).thenReturn(netflixCheckpoint);
        when(progressService.getOrCreateCheckpoint("BR", 119)).thenReturn(primeCheckpoint);
        when(progressService.recordPageSuccess(any(), anyInt(), anyInt(), eq(NOW)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tmdbClient.discoverMovies(anyInt(), eq(1)))
                .thenReturn(new TmdbDiscoverPage(List.of(movie), 20));
        when(tmdbClient.findAvailability(550L)).thenReturn(new TmdbAvailability(
                "https://www.themoviedb.org/movie/550/watch",
                List.of(new TmdbOfferData(8, "Netflix", "/netflix.jpg", 0, MonetizationType.FLATRATE))
        ));
        when(persistenceService.upsert(eq(movie), any(), eq("BR"), eq(NOW))).thenReturn(1);

        var result = service.synchronize(CatalogSyncTrigger.SCHEDULED);

        assertThat(result.status()).isEqualTo(CatalogSyncStatus.SUCCEEDED);
        assertThat(result.providersRequested()).isEqualTo(2);
        assertThat(result.providersCompleted()).isEqualTo(2);
        assertThat(result.moviesDiscovered()).isEqualTo(2);
        assertThat(result.moviesImported()).isEqualTo(1);
        assertThat(result.moviesSkipped()).isEqualTo(1);
        verify(tmdbClient).findAvailability(550L);
        verify(progressService).recordPageSuccess(netflixCheckpoint, 1, 2, NOW);
        verify(progressService).recordPageSuccess(primeCheckpoint, 1, 2, NOW);
        verify(progressService).finishRun(eq(run), eq(result), eq(NOW));
        verify(persistenceService).activateOnly(eq(java.util.Set.of(8, 119)));
        verify(leaseRepository).release(eq("TMDB_CATALOG"), any());
    }

    @Test
    void shouldSkipWithoutCallingTmdbWhenAnotherInstanceOwnsTheLease() {
        when(leaseRepository.tryAcquire(eq("TMDB_CATALOG"), any(), any())).thenReturn(false);

        var result = service.synchronize(CatalogSyncTrigger.SCHEDULED);

        assertThat(result.status()).isEqualTo(CatalogSyncStatus.SKIPPED);
        verify(tmdbClient, never()).listMovieProviders();
        verify(leaseRepository, never()).release(anyString(), any());
    }

    private TmdbMovieData movie(long id) {
        return new TmdbMovieData(
                id,
                "Clube da Luta",
                "Fight Club",
                "Sinopse",
                "/poster.jpg",
                LocalDate.of(1999, 10, 15),
                new BigDecimal("8.4"),
                30_000,
                List.of(18, 53),
                false,
                "en"
        );
    }
}
