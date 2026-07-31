package com.roletadefilmes.catalog.integration.tmdb;

import java.util.List;

public record TmdbAvailability(
        String attributionUrl,
        List<TmdbOfferData> offers
) {
    public static TmdbAvailability empty() {
        return new TmdbAvailability(null, List.of());
    }
}
