package com.roletadefilmes.roulette.persistence.repository;

import com.roletadefilmes.roulette.persistence.entity.RouletteSpinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RouletteSpinRepository extends JpaRepository<RouletteSpinEntity, UUID> {

    Optional<RouletteSpinEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
