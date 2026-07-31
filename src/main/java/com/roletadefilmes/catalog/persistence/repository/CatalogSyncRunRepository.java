package com.roletadefilmes.catalog.persistence.repository;

import com.roletadefilmes.catalog.persistence.entity.CatalogSyncRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CatalogSyncRunRepository extends JpaRepository<CatalogSyncRunEntity, UUID> {
}
