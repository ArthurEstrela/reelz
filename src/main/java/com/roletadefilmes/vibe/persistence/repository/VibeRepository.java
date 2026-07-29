package com.roletadefilmes.vibe.persistence.repository;

import com.roletadefilmes.vibe.persistence.entity.VibeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VibeRepository extends JpaRepository<VibeEntity, UUID> {

    Optional<VibeEntity> findBySlugAndActiveTrue(String slug);

    List<VibeEntity> findAllByActiveTrueOrderByLabelAsc();
}
