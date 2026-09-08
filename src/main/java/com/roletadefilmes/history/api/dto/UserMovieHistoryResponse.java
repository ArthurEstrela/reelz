package com.roletadefilmes.history.api.dto;

import com.roletadefilmes.history.domain.UserMovieStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserMovieHistoryResponse(
        UUID id,
        Long movieId,
        String title,
        String overview,
        String posterPath,
        LocalDate releaseDate,
        BigDecimal tmdbRating,
        UserMovieStatus status,
        Instant watchedAt,
        Integer rating
) {
}
