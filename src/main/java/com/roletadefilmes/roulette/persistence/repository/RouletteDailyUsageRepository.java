package com.roletadefilmes.roulette.persistence.repository;

import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RouletteDailyUsageRepository extends JpaRepository<RouletteDailyUsageEntity, UUID> {

    Optional<RouletteDailyUsageEntity> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);
}
