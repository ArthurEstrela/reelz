package com.roletadefilmes.catalog.persistence.repository;

import com.roletadefilmes.catalog.persistence.entity.CatalogSyncCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CatalogSyncCheckpointRepository extends JpaRepository<CatalogSyncCheckpointEntity, UUID> {

    Optional<CatalogSyncCheckpointEntity> findBySourceAndRegionAndExternalProviderId(
            String source,
            String region,
            int externalProviderId
    );
}
