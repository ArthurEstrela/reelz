package com.roletadefilmes.roulette.api.dto;

import com.roletadefilmes.streaming.domain.MonetizationType;

import java.util.UUID;

public record StreamingAvailabilityResponse(
        UUID providerId,
        Integer tmdbProviderId,
        String providerName,
        String logoPath,
        MonetizationType monetizationType,
        String attributionUrl
) {
}
