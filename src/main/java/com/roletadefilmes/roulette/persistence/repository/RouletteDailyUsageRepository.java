package com.roletadefilmes.roulette.persistence.repository;

import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RouletteDailyUsageRepository extends JpaRepository<RouletteDailyUsageEntity, UUID> {

    Optional<RouletteDailyUsageEntity> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);

    @Modifying
    @Query(value = """
            INSERT INTO roulette_daily_usage (
                id,
                user_id,
                usage_date,
                base_spins_used,
                rewarded_spins_granted,
                rewarded_spins_used,
                timezone_snapshot,
                version,
                created_at,
                updated_at
            ) VALUES (
                :id,
                :userId,
                :usageDate,
                0,
                0,
                0,
                :timezone,
                0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (user_id, usage_date) DO NOTHING
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("usageDate") LocalDate usageDate,
            @Param("timezone") String timezone
    );
}
