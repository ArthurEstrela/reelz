package com.roletadefilmes.catalog.persistence;

import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncCheckpointRepository;
import com.roletadefilmes.catalog.persistence.repository.CatalogSyncLeaseRepository;
import com.roletadefilmes.catalog.service.TmdbCatalogProgressService;
import com.roletadefilmes.support.PostgresRepositoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({CatalogSyncLeaseRepository.class, TmdbCatalogProgressService.class})
class CatalogSyncPersistenceIntegrationTest extends PostgresRepositoryIntegrationTest {

    @Autowired
    private CatalogSyncLeaseRepository leaseRepository;

    @Autowired
    private CatalogSyncCheckpointRepository checkpointRepository;

    @Autowired
    private TmdbCatalogProgressService progressService;

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
}
