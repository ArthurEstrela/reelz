package com.roletadefilmes.catalog.integration.tmdb;

public record TmdbProviderData(
        int providerId,
        String name,
        String logoPath,
        int displayPriority
) {
}
