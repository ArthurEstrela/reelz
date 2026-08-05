package com.roletadefilmes.admin.persistence;

import com.roletadefilmes.admin.api.dto.DailyAnalyticsResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class AnalyticsQueryRepository {

    private static final String SUMMARY_SQL = """
            WITH
            new_users AS (
                SELECT id, created_at, onboarding_completed_at
                  FROM user_account
                 WHERE deleted_at IS NULL
                   AND created_at >= :fromInstant
                   AND created_at < :toInstant
            ),
            activity AS (
                SELECT user_id FROM roulette_spin
                 WHERE created_at >= :fromInstant AND created_at < :toInstant
                UNION
                SELECT user_id FROM product_event
                 WHERE occurred_at >= :fromInstant AND occurred_at < :toInstant
            ),
            decision_sessions AS (
                SELECT DISTINCT session_id
                  FROM product_event
                 WHERE event_type = 'WATCH_PROVIDER_CLICKED'
                   AND occurred_at >= :fromInstant
                   AND occurred_at < :toInstant
            ),
            spins_by_decision_session AS (
                SELECT decision.session_id, COUNT(spin.id) AS spin_count
                  FROM decision_sessions decision
                  LEFT JOIN roulette_spin spin
                    ON spin.status = 'SUCCEEDED'
                   AND spin.filters ->> 'sessionId' = decision.session_id::text
                   AND spin.created_at >= :fromInstant
                   AND spin.created_at < :toInstant
                 GROUP BY decision.session_id
            ),
            d7_cohort AS (
                SELECT id, created_at
                  FROM user_account
                 WHERE deleted_at IS NULL
                   AND created_at >= :fromInstant
                   AND created_at < :toInstant - INTERVAL '7 days'
            )
            SELECT
                (SELECT COUNT(*) FROM user_account WHERE deleted_at IS NULL) AS total_users,
                (SELECT COUNT(*) FROM new_users) AS new_users,
                (SELECT COUNT(*) FROM new_users WHERE onboarding_completed_at IS NOT NULL)
                    AS onboarding_completed_users,
                (SELECT COUNT(DISTINCT spin.user_id)
                   FROM roulette_spin spin
                   JOIN new_users users ON users.id = spin.user_id
                  WHERE spin.status = 'SUCCEEDED') AS first_spin_users,
                (SELECT COUNT(*) FROM activity) AS active_users,
                (SELECT COUNT(*) FROM roulette_spin
                  WHERE status = 'SUCCEEDED'
                    AND created_at >= :fromInstant AND created_at < :toInstant) AS successful_spins,
                (SELECT COUNT(DISTINCT session_id) FROM product_event
                  WHERE event_type = 'HOME_VIEWED'
                    AND occurred_at >= :fromInstant AND occurred_at < :toInstant) AS home_sessions,
                (SELECT COUNT(*) FROM decision_sessions) AS decided_sessions,
                (SELECT COUNT(*) FROM product_event
                  WHERE event_type = 'WATCH_PROVIDER_CLICKED'
                    AND occurred_at >= :fromInstant AND occurred_at < :toInstant) AS provider_clicks,
                (SELECT COUNT(*) FROM user_movie_history
                  WHERE status = 'WATCHED'
                    AND updated_at >= :fromInstant AND updated_at < :toInstant) AS watched_movies,
                (SELECT COUNT(*) FROM user_movie_history
                  WHERE status = 'WATCHLIST'
                    AND updated_at >= :fromInstant AND updated_at < :toInstant) AS watchlisted_movies,
                (SELECT COUNT(DISTINCT user_id) FROM product_event
                  WHERE event_type = 'COUPLE_MODE_INTERESTED'
                    AND occurred_at >= :fromInstant AND occurred_at < :toInstant)
                    AS couple_mode_interested_users,
                (SELECT COUNT(DISTINCT user_id) FROM product_event
                  WHERE event_type = 'GROUP_MODE_INTERESTED'
                    AND occurred_at >= :fromInstant AND occurred_at < :toInstant)
                    AS group_mode_interested_users,
                (SELECT COUNT(*) FROM social_room
                  WHERE created_at >= :fromInstant AND created_at < :toInstant)
                    AS social_rooms_created,
                (SELECT COUNT(DISTINCT room.id)
                   FROM social_room room
                   JOIN social_room_spin spin ON spin.room_id = room.id
                  WHERE room.created_at >= :fromInstant AND room.created_at < :toInstant)
                    AS social_rooms_with_spin,
                (SELECT COUNT(*) FROM social_room_spin
                  WHERE created_at >= :fromInstant AND created_at < :toInstant)
                    AS social_spins,
                (SELECT COUNT(DISTINCT member.user_id)
                   FROM social_room_member member
                   JOIN social_room room ON room.id = member.room_id
                  WHERE room.created_at >= :fromInstant AND room.created_at < :toInstant)
                    AS social_participants,
                (SELECT COUNT(*) FROM d7_cohort) AS d7_eligible_users,
                (SELECT COUNT(*) FROM d7_cohort cohort
                  WHERE EXISTS (
                      SELECT 1 FROM roulette_spin spin
                       WHERE spin.user_id = cohort.id
                         AND spin.created_at >= cohort.created_at + INTERVAL '7 days'
                         AND spin.created_at < cohort.created_at + INTERVAL '14 days'
                  )) AS d7_retained_users,
                COALESCE((SELECT AVG(spin_count) FROM spins_by_decision_session), 0)
                    AS average_spins_per_decision
            """;

    private static final String DAILY_SQL = """
            WITH days AS (
                SELECT generate_series(
                    CAST(:fromDate AS date),
                    CAST(:toDate AS date),
                    INTERVAL '1 day'
                )::date AS day
            )
            SELECT
                days.day,
                (SELECT COUNT(*) FROM user_account users
                  WHERE users.created_at >= days.day::timestamptz
                    AND users.created_at < (days.day + 1)::timestamptz) AS registrations,
                (SELECT COUNT(*) FROM roulette_spin spins
                  WHERE spins.status = 'SUCCEEDED'
                    AND spins.created_at >= days.day::timestamptz
                    AND spins.created_at < (days.day + 1)::timestamptz) AS successful_spins,
                (SELECT COUNT(DISTINCT events.session_id) FROM product_event events
                  WHERE events.event_type = 'WATCH_PROVIDER_CLICKED'
                    AND events.occurred_at >= days.day::timestamptz
                    AND events.occurred_at < (days.day + 1)::timestamptz) AS decisions
              FROM days
             ORDER BY days.day
            """;

    private final JdbcClient jdbcClient;

    public AnalyticsQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public AnalyticsSummary summary(Instant from, Instant to) {
        return jdbcClient.sql(SUMMARY_SQL)
                .param("fromInstant", from.atOffset(ZoneOffset.UTC))
                .param("toInstant", to.atOffset(ZoneOffset.UTC))
                .query((resultSet, rowNumber) -> new AnalyticsSummary(
                        resultSet.getLong("total_users"),
                        resultSet.getLong("new_users"),
                        resultSet.getLong("onboarding_completed_users"),
                        resultSet.getLong("first_spin_users"),
                        resultSet.getLong("active_users"),
                        resultSet.getLong("successful_spins"),
                        resultSet.getLong("home_sessions"),
                        resultSet.getLong("decided_sessions"),
                        resultSet.getLong("provider_clicks"),
                        resultSet.getLong("watched_movies"),
                        resultSet.getLong("watchlisted_movies"),
                        resultSet.getLong("couple_mode_interested_users"),
                        resultSet.getLong("group_mode_interested_users"),
                        resultSet.getLong("social_rooms_created"),
                        resultSet.getLong("social_rooms_with_spin"),
                        resultSet.getLong("social_spins"),
                        resultSet.getLong("social_participants"),
                        resultSet.getLong("d7_eligible_users"),
                        resultSet.getLong("d7_retained_users"),
                        resultSet.getDouble("average_spins_per_decision")
                ))
                .single();
    }

    public List<DailyAnalyticsResponse> daily(LocalDate from, LocalDate to) {
        return jdbcClient.sql(DAILY_SQL)
                .param("fromDate", from)
                .param("toDate", to)
                .query((resultSet, rowNumber) -> new DailyAnalyticsResponse(
                        resultSet.getObject("day", LocalDate.class),
                        resultSet.getLong("registrations"),
                        resultSet.getLong("successful_spins"),
                        resultSet.getLong("decisions")
                ))
                .list();
    }

    public record AnalyticsSummary(
            long totalUsers,
            long newUsers,
            long onboardingCompletedUsers,
            long firstSpinUsers,
            long activeUsers,
            long successfulSpins,
            long homeSessions,
            long decidedSessions,
            long providerClicks,
            long watchedMovies,
            long watchlistedMovies,
            long coupleModeInterestedUsers,
            long groupModeInterestedUsers,
            long socialRoomsCreated,
            long socialRoomsWithSpin,
            long socialSpins,
            long socialParticipants,
            long d7EligibleUsers,
            long d7RetainedUsers,
            double averageSpinsPerDecision
    ) {
    }
}
