package com.roletadefilmes.history.api.dto;

import com.roletadefilmes.history.domain.UserMovieStatus;

import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        Long movieId,
        UserMovieStatus status,
        Instant watchedAt,
        Integer rating,
        Instant createdAt,
        Instant updatedAt
) {
}
