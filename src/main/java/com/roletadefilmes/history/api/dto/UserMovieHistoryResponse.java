package com.roletadefilmes.history.api.dto;

import com.roletadefilmes.history.domain.UserMovieStatus;

import java.time.Instant;
import java.util.UUID;

public record UserMovieHistoryResponse(
        UUID id,
        UUID movieId,
        Long tmdbMovieId,
        UserMovieStatus status,
        Instant watchedAt,
        Integer userRating,
        Instant createdAt,
        Instant updatedAt
) {
}
