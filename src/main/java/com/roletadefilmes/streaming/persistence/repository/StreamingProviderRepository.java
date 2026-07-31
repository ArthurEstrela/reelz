package com.roletadefilmes.streaming.persistence.repository;

import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamingProviderRepository extends JpaRepository<StreamingProviderEntity, UUID> {

    Optional<StreamingProviderEntity> findByTmdbProviderId(Integer tmdbProviderId);

    List<StreamingProviderEntity> findAllByActiveTrueOrderByDisplayPriorityAsc();

    @Query(value = """
            SELECT DISTINCT sp.*
              FROM streaming_provider sp
              JOIN movie_streaming_offer offer ON offer.provider_id = sp.id
             WHERE sp.active = TRUE
               AND offer.country_code = :countryCode
               AND offer.monetization_type IN ('FLATRATE', 'FREE', 'ADS')
             ORDER BY sp.display_priority ASC, sp.name ASC
            """, nativeQuery = true)
    List<StreamingProviderEntity> findEligibleForCountry(@Param("countryCode") String countryCode);
}
