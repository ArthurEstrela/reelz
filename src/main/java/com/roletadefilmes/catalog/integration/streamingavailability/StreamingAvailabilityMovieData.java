package com.roletadefilmes.catalog.integration.streamingavailability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StreamingAvailabilityMovieData(
        String externalId,
        long tmdbId,
        String imdbId,
        String title,
        String originalTitle,
        String overview,
        String posterUrl,
        LocalDate releaseDate,
        BigDecimal voteAverage,
        List<Integer> genreIds,
        Integer runtimeMinutes,
        List<StreamingAvailabilityOfferData> offers
) {
    public StreamingAvailabilityMovieData {
        genreIds = genreIds == null ? List.of() : List.copyOf(genreIds);
        offers = offers == null ? List.of() : List.copyOf(offers);
    }
}
