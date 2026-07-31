package com.roletadefilmes.catalog.integration.tmdb;

import com.roletadefilmes.streaming.domain.MonetizationType;

public record TmdbOfferData(
        int providerId,
        String providerName,
        String logoPath,
        int displayPriority,
        MonetizationType monetizationType
) {
}
