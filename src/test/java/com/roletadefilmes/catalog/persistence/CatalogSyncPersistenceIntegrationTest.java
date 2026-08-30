package com.roletadefilmes.catalog.persistence;

import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncCheckpointRepository;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncLeaseRepository;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityMovieData;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityOfferData;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityProviderData;
import com.roletadefilmes.catalog.service.StreamingAvailabilityCatalogPersistenceService;
import com.roletadefilmes.catalog.service.TmdbCatalogProgressService;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.streaming.domain.MonetizationType;
import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.support.PostgresRepositoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        CatalogSyncLeaseRepository.class,
        TmdbCatalogProgressService.class,
        StreamingAvailabilityCatalogPersistenceService.class
})
class CatalogSyncPersistenceIntegrationTest extends PostgresRepositoryIntegrationTest {

    @Autowired
    private CatalogSyncLeaseRepository leaseRepository;

    @Autowired
    private CatalogSyncCheckpointRepository checkpointRepository;

    @Autowired
    private TmdbCatalogProgressService progressService;

    @Autowired
    private StreamingAvailabilityCatalogPersistenceService streamingAvailabilityPersistenceService;

    @Autowired
    private StreamingProviderRepository providerRepository;

    @Autowired
    private MovieCacheRepository movieRepository;

    @Autowired
    private MovieStreamingOfferRepository offerRepository;

    @Test
    void shouldAllowOnlyOneLeaseOwnerAtATime() {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();

        assertThat(leaseRepository.tryAcquire("TMDB_CATALOG", firstOwner, Duration.ofMinutes(5))).isTrue();
        assertThat(leaseRepository.tryAcquire("TMDB_CATALOG", secondOwner, Duration.ofMinutes(5))).isFalse();

        leaseRepository.release("TMDB_CATALOG", firstOwner);

        assertThat(leaseRepository.tryAcquire("TMDB_CATALOG", secondOwner, Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void shouldPersistTheNextProviderPageAsCheckpoint() {
        var checkpoint = checkpointRepository.saveAndFlush(
                new CatalogSyncCheckpointEntity("TMDB", "BR", 8)
        );
        Instant syncedAt = Instant.parse("2026-07-30T12:00:00Z");

        checkpoint.recordSuccess(3, 4, syncedAt);
        checkpointRepository.saveAndFlush(checkpoint);

        var persisted = checkpointRepository.findBySourceAndRegionAndExternalProviderId("TMDB", "BR", 8)
                .orElseThrow();
        assertThat(persisted.getNextPage()).isEqualTo(4);
        assertThat(persisted.getLastSuccessfulPage()).isEqualTo(3);
        assertThat(persisted.getLastSuccessAt()).isEqualTo(syncedAt);
        assertThat(persisted.getVersion()).isPositive();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldReturnTheCurrentVersionWhenUpdatingDetachedCheckpointAcrossPages() {
        Instant syncedAt = Instant.parse("2026-07-30T12:00:00Z");
        var checkpoint = progressService.getOrCreateCheckpoint("BR", 999);

        checkpoint = progressService.recordPageSuccess(checkpoint, 1, 2, syncedAt);
        checkpoint = progressService.recordPageSuccess(checkpoint, 2, 3, syncedAt.plusSeconds(1));

        assertThat(checkpoint.getNextPage()).isEqualTo(3);
        assertThat(checkpoint.getVersion()).isEqualTo(2);
    }

    @Test
    void shouldPersistStreamingAvailabilityCursorAndBootstrapState() {
        Instant syncedAt = Instant.parse("2026-08-28T12:00:00Z");
        var checkpoint = checkpointRepository.saveAndFlush(
                new CatalogSyncCheckpointEntity("STREAMING_AVAILABILITY", "BR", 1)
        );

        checkpoint.recordBootstrapPageSuccess(1, "opaque-cursor", syncedAt);
        checkpointRepository.saveAndFlush(checkpoint);

        var persisted = checkpointRepository.findBySourceAndRegionAndExternalProviderId(
                        "STREAMING_AVAILABILITY",
                        "BR",
                        1
                )
                .orElseThrow();
        assertThat(persisted.getNextCursor()).isEqualTo("opaque-cursor");
        assertThat(persisted.getNextPage()).isEqualTo(2);
        assertThat(persisted.isBootstrapCompleted()).isFalse();

        persisted.completeBootstrap(2, syncedAt.plusSeconds(60));
        checkpointRepository.saveAndFlush(persisted);

        var completed = checkpointRepository.findById(persisted.getId()).orElseThrow();
        assertThat(completed.isBootstrapCompleted()).isTrue();
        assertThat(completed.getNextCursor()).isNull();
        assertThat(completed.getNextPage()).isEqualTo(1);
    }

    @Test
    void shouldReuseTmdbProviderAndPersistStreamingAvailabilityMovieAndOffer() {
        providerRepository.saveAndFlush(new StreamingProviderEntity(8, "Netflix"));
        streamingAvailabilityPersistenceService.synchronizeProviders(
                List.of(new StreamingAvailabilityProviderData(
                        "netflix",
                        "Netflix",
                        "https://cdn.example/netflix.png",
                        0
                )),
                false
        );
        Instant syncedAt = Instant.parse("2026-08-28T12:00:00Z");
        var movie = new StreamingAvailabilityMovieData(
                "movie-550",
                550L,
                "tt0137523",
                "Fight Club",
                "Fight Club",
                "Overview",
                "https://cdn.example/fight-club.jpg",
                LocalDate.of(1999, 1, 1),
                new BigDecimal("8.4"),
                List.of(18, 53),
                139,
                List.of(new StreamingAvailabilityOfferData(
                        "netflix",
                        "Netflix",
                        "https://cdn.example/netflix.png",
                        MonetizationType.FLATRATE,
                        "https://www.netflix.com/watch/550",
                        syncedAt.minusSeconds(60),
                        null
                ))
        );

        int importedOffers = streamingAvailabilityPersistenceService.upsertMovie(
                movie,
                Set.of("netflix"),
                "BR",
                syncedAt
        );

        assertThat(importedOffers).isEqualTo(1);
        assertThat(providerRepository.findAll()).singleElement().satisfies(provider -> {
            assertThat(provider.getTmdbProviderId()).isEqualTo(8);
            assertThat(provider.getStreamingAvailabilityServiceId()).isEqualTo("netflix");
            assertThat(provider.getLogoPath()).isEqualTo("https://cdn.example/netflix.png");
        });
        var persistedMovie = movieRepository.findByTmdbId(550L).orElseThrow();
        assertThat(persistedMovie.getMetadataSource()).isEqualTo("STREAMING_AVAILABILITY");
        assertThat(persistedMovie.getExternalCatalogId()).isEqualTo("movie-550");
        assertThat(persistedMovie.getPosterPath()).isEqualTo("https://cdn.example/fight-club.jpg");
        assertThat(offerRepository.findAllByMovieIdAndCountryCode(persistedMovie.getId(), "BR"))
                .singleElement()
                .satisfies(offer -> {
                    assertThat(offer.getCatalogSource()).isEqualTo("STREAMING_AVAILABILITY");
                    assertThat(offer.getAttributionUrl()).isEqualTo("https://www.netflix.com/watch/550");
                });
    }
}
