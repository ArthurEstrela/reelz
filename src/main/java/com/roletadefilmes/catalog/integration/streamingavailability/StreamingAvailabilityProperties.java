package com.roletadefilmes.catalog.integration.streamingavailability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "reelz.streaming-availability")
public record StreamingAvailabilityProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String country,
        String outputLanguage,
        List<String> catalogs,
        int bootstrapPagesPerRun,
        int changesPagesPerRun,
        boolean deactivateUnconfiguredProviders,
        boolean syncOnStartup,
        boolean scheduledSyncEnabled,
        String syncCron,
        String syncZone,
        Duration leaseDuration,
        int maxAttempts,
        Duration retryBaseDelay,
        Duration connectTimeout,
        Duration readTimeout
) {
    public StreamingAvailabilityProperties {
        catalogs = catalogs == null ? List.of() : catalogs.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
