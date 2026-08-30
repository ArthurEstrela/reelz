package com.roletadefilmes.catalog.integration.streamingavailability;

import java.util.Map;

final class StreamingAvailabilityGenreMapping {

    private static final Map<String, Integer> TMDB_GENRE_IDS = Map.ofEntries(
            Map.entry("action", 28),
            Map.entry("adventure", 12),
            Map.entry("animation", 16),
            Map.entry("comedy", 35),
            Map.entry("crime", 80),
            Map.entry("documentary", 99),
            Map.entry("drama", 18),
            Map.entry("family", 10751),
            Map.entry("fantasy", 14),
            Map.entry("history", 36),
            Map.entry("horror", 27),
            Map.entry("music", 10402),
            Map.entry("mystery", 9648),
            Map.entry("news", 10763),
            Map.entry("reality", 10764),
            Map.entry("romance", 10749),
            Map.entry("scifi", 878),
            Map.entry("talk", 10767),
            Map.entry("thriller", 53),
            Map.entry("war", 10752),
            Map.entry("western", 37)
    );

    private StreamingAvailabilityGenreMapping() {
    }

    static Integer toTmdbId(String genreId) {
        return TMDB_GENRE_IDS.get(genreId);
    }
}
