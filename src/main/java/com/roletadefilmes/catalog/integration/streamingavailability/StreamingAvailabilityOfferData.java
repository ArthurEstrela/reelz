package com.roletadefilmes.catalog.integration.streamingavailability;

import com.roletadefilmes.streaming.domain.MonetizationType;

import java.time.Instant;

public record StreamingAvailabilityOfferData(
        String serviceId,
        String providerName,
        String logoUrl,
        MonetizationType monetizationType,
        String deepLink,
        Instant availableFrom,
        Instant availableUntil
) {
}
