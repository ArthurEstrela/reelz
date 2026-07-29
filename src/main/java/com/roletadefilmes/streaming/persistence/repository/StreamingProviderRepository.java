package com.roletadefilmes.streaming.persistence.repository;

import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamingProviderRepository extends JpaRepository<StreamingProviderEntity, UUID> {

    Optional<StreamingProviderEntity> findByTmdbProviderId(Integer tmdbProviderId);

    List<StreamingProviderEntity> findAllByActiveTrueOrderByDisplayPriorityAsc();
}
