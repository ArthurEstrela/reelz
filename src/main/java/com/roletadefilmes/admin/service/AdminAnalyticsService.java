package com.roletadefilmes.admin.service;

import com.roletadefilmes.admin.api.dto.AnalyticsOverviewResponse;
import com.roletadefilmes.admin.api.dto.RecentFeedbackResponse;
import com.roletadefilmes.admin.persistence.AnalyticsQueryRepository;
import com.roletadefilmes.feedback.persistence.repository.BetaFeedbackRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class AdminAnalyticsService {

    private final AnalyticsQueryRepository repository;
    private final BetaFeedbackRepository feedbackRepository;
    private final Clock clock;

    public AdminAnalyticsService(
            AnalyticsQueryRepository repository,
            BetaFeedbackRepository feedbackRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.feedbackRepository = feedbackRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(int days) {
        var to = Instant.now(clock);
        var from = to.minus(days, ChronoUnit.DAYS);
        var summary = repository.summary(from, to);
        var fromDate = from.atZone(ZoneOffset.UTC).toLocalDate();
        var toDate = to.atZone(ZoneOffset.UTC).toLocalDate();
        var feedbackCount = feedbackRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);
        var averageFeedbackScore = feedbackRepository.averageScore(from, to);
        var recentFeedback = feedbackRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        from,
                        to,
                        PageRequest.of(0, 20)
                )
                .stream()
                .map(feedback -> new RecentFeedbackResponse(
                        feedback.getCreatedAt(),
                        feedback.getScore(),
                        feedback.getMessage()
                ))
                .toList();

        return new AnalyticsOverviewResponse(
                from,
                to,
                summary.totalUsers(),
                summary.newUsers(),
                summary.onboardingCompletedUsers(),
                summary.firstSpinUsers(),
                summary.activeUsers(),
                summary.successfulSpins(),
                summary.homeSessions(),
                summary.decidedSessions(),
                summary.providerClicks(),
                summary.watchedMovies(),
                summary.watchlistedMovies(),
                summary.coupleModeInterestedUsers(),
                summary.groupModeInterestedUsers(),
                summary.d7EligibleUsers(),
                summary.d7RetainedUsers(),
                percentage(summary.onboardingCompletedUsers(), summary.newUsers()),
                percentage(summary.decidedSessions(), summary.homeSessions()),
                percentage(summary.d7RetainedUsers(), summary.d7EligibleUsers()),
                round(summary.averageSpinsPerDecision()),
                feedbackCount,
                round(averageFeedbackScore == null ? 0 : averageFeedbackScore),
                recentFeedback,
                repository.daily(fromDate, toDate)
        );
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return round((double) numerator * 100 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
