package com.roletadefilmes.catalog.integration.streamingavailability;

public record StreamingAvailabilityProviderData(
        String serviceId,
        String name,
        String logoUrl,
        int displayPriority
) {
}
