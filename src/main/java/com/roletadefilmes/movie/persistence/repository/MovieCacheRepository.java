package com.roletadefilmes.movie.persistence.repository;

import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

public interface MovieCacheRepository extends JpaRepository<MovieCacheEntity, UUID> {

    Optional<MovieCacheEntity> findByTmdbId(Long tmdbId);

    List<MovieCacheEntity> findAllByTmdbIdIn(Set<Long> tmdbIds);

    @Query(value = """
            SELECT m.*
              FROM movie_cache m
             WHERE m.adult = FALSE
               AND m.poster_path IS NOT NULL
               AND NOT EXISTS (
                    SELECT 1
                      FROM user_movie_history h
                     WHERE h.user_id = CAST(:userId AS UUID)
                       AND h.movie_id = m.id
                       AND h.status = 'WATCHED'
               )
               AND EXISTS (
                    SELECT 1
                      FROM movie_streaming_offer o
                      JOIN streaming_provider sp ON sp.id = o.provider_id
                     WHERE o.movie_id = m.id
                       AND o.country_code = :countryCode
                       AND o.monetization_type IN ('FLATRATE', 'FREE', 'ADS')
                       AND sp.active = TRUE
                       AND (o.available_from IS NULL OR o.available_from <= CURRENT_TIMESTAMP)
                       AND (o.available_until IS NULL OR o.available_until > CURRENT_TIMESTAMP)
               )
             ORDER BY m.vote_count DESC, m.vote_average DESC NULLS LAST, m.tmdb_id
             LIMIT :limit
            """, nativeQuery = true)
    List<MovieCacheEntity> findPopularForOnboarding(
            @Param("userId") UUID userId,
            @Param("countryCode") String countryCode,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT m.*
              FROM movie_cache m
             WHERE m.adult = FALSE
               AND (:genreId IS NULL OR CAST(:genreId AS INTEGER) = ANY(m.genre_ids))
               AND (
                    :vibeId IS NULL
                    OR EXISTS (
                        SELECT 1
                          FROM vibe v
                         WHERE v.id = CAST(:vibeId AS UUID)
                           AND v.active = TRUE
                           AND m.genre_ids && v.genre_ids
                    )
               )
               AND NOT EXISTS (
                    SELECT 1
                      FROM user_movie_history h
                     WHERE h.user_id = CAST(:userId AS UUID)
                       AND h.movie_id = m.id
                       AND h.status = 'WATCHED'
                )
               AND EXISTS (
                    SELECT 1
                      FROM movie_streaming_offer o
                      JOIN streaming_provider sp ON sp.id = o.provider_id
                     WHERE o.movie_id = m.id
                       AND o.provider_id IN (:providerIds)
                       AND o.country_code = :countryCode
                       AND o.monetization_type IN ('FLATRATE', 'FREE', 'ADS')
                       AND sp.active = TRUE
                       AND (o.available_from IS NULL OR o.available_from <= CURRENT_TIMESTAMP)
                       AND (o.available_until IS NULL OR o.available_until > CURRENT_TIMESTAMP)
               )
             ORDER BY RANDOM()
             LIMIT 1
            """, nativeQuery = true)
    Optional<MovieCacheEntity> findRandomAvailableMovie(
            @Param("userId") UUID userId,
            @Param("providerIds") List<UUID> providerIds,
            @Param("countryCode") String countryCode,
            @Param("genreId") Integer genreId,
            @Param("vibeId") UUID vibeId
    );
}
