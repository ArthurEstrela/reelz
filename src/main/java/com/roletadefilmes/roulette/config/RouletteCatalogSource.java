package com.roletadefilmes.roulette.config;

public enum RouletteCatalogSource {
    ALL,
    TMDB,
    STREAMING_AVAILABILITY;

    public boolean accepts(String catalogSource) {
        return this == ALL || name().equalsIgnoreCase(catalogSource);
    }
}
