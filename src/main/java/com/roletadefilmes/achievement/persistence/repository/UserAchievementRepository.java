package com.roletadefilmes.achievement.persistence.repository;

import com.roletadefilmes.achievement.persistence.entity.UserAchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, UUID> {

    @Query(value = """
            SELECT 1
              FROM pg_advisory_xact_lock(hashtext(CAST(:userId AS text)))
            """, nativeQuery = true)
    int lockUserProgress(@Param("userId") UUID userId);

    @Query("""
            SELECT progress
              FROM UserAchievementEntity progress
              JOIN FETCH progress.achievement
             WHERE progress.user.id = :userId
            """)
    List<UserAchievementEntity> findAllWithDefinitionByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM user_movie_history
             WHERE user_id = :userId
               AND status = :status
            """, nativeQuery = true)
    long countHistory(@Param("userId") UUID userId, @Param("status") String status);

    @Query(value = """
            SELECT COUNT(DISTINCT genre_id)
              FROM user_movie_history history
              JOIN movie_cache movie ON movie.id = history.movie_id
              CROSS JOIN LATERAL unnest(movie.genre_ids) AS genre_id
             WHERE history.user_id = :userId
               AND history.status = 'WATCHED'
            """, nativeQuery = true)
    long countWatchedGenres(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM roulette_spin
             WHERE user_id = :userId
               AND status = 'SUCCEEDED'
            """, nativeQuery = true)
    long countSuccessfulSpins(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(DISTINCT date_trunc('week', spin.created_at AT TIME ZONE account.timezone))
              FROM roulette_spin spin
              JOIN user_account account ON account.id = spin.user_id
             WHERE spin.user_id = :userId
               AND spin.status = 'SUCCEEDED'
            """, nativeQuery = true)
    long countActiveWeeks(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM product_event
             WHERE user_id = :userId
               AND event_type = 'WATCH_PROVIDER_CLICKED'
            """, nativeQuery = true)
    long countProviderOpens(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM social_room_spin_participant participant
             WHERE participant.user_id = :userId
               AND participant.room_type = 'COUPLE'
            """, nativeQuery = true)
    long countCoupleSpins(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM social_room_spin_participant participant
             WHERE participant.user_id = :userId
               AND participant.room_type = 'GROUP'
               AND participant.participant_count >= 3
            """, nativeQuery = true)
    long countGroupSpinsWithThreeMembers(@Param("userId") UUID userId);
}
