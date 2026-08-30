package com.roletadefilmes.catalog.integration.streamingavailability;

import java.util.List;

public record StreamingAvailabilityMoviePage(
        List<StreamingAvailabilityMovieData> movies,
        boolean hasMore,
        String nextCursor
) {
    public StreamingAvailabilityMoviePage {
        movies = movies == null ? List.of() : List.copyOf(movies);
    }
}
