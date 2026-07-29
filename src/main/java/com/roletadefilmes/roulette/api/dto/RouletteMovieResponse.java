package com.roletadefilmes.roulette.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouletteMovieResponse(
        UUID id,
        Long tmdbId,
        String title,
        String overview,
        String posterPath,
        LocalDate releaseDate,
        BigDecimal tmdbRating,
        List<StreamingAvailabilityResponse> streamingAvailability
) {
}
