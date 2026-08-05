package com.roletadefilmes.admin.api.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsOverviewResponse(
        Instant from,
        Instant to,
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
        double activationRate,
        double decisionRate,
        double d7RetentionRate,
        double averageSpinsPerDecision,
        long feedbackCount,
        double averageFeedbackScore,
        List<RecentFeedbackResponse> recentFeedback,
        List<DailyAnalyticsResponse> daily
) {
}
