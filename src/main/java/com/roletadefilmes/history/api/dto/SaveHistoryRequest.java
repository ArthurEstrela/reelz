package com.roletadefilmes.history.api.dto;

import com.roletadefilmes.history.domain.UserMovieStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaveHistoryRequest(
        @NotNull(message = "O ID do filme no TMDB é obrigatório")
        @Positive(message = "O ID do filme no TMDB deve ser positivo")
        Long movieId,

        @NotNull(message = "O status é obrigatório")
        UserMovieStatus status
) {
}
