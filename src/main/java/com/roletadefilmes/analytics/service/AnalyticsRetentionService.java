package com.roletadefilmes.analytics.service;

import com.roletadefilmes.analytics.persistence.repository.ProductEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AnalyticsRetentionService {

    private final ProductEventRepository eventRepository;
    private final Clock clock;
    private final int retentionDays;

    public AnalyticsRetentionService(
            ProductEventRepository eventRepository,
            Clock clock,
            @Value("${reelz.analytics.retention-days:180}") int retentionDays
    ) {
        this.eventRepository = eventRepository;
        this.clock = clock;
        this.retentionDays = Math.max(30, retentionDays);
    }

    @Scheduled(cron = "${reelz.analytics.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    @Transactional
    public void removeExpiredEvents() {
        eventRepository.deleteByOccurredAtBefore(
                Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS)
        );
    }
}
