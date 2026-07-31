package com.roletadefilmes.catalog.integration.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "reelz.tmdb")
public record TmdbProperties(
        String readAccessToken,
        String language,
        String region,
        int pagesPerProvider,
        int maxProviders,
        List<Integer> providerIds,
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
    public TmdbProperties {
        providerIds = providerIds == null ? List.of() : List.copyOf(providerIds);
    }
}
