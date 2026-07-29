package com.roletadefilmes.history.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roletadefilmes.history.domain.UserMovieStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record SaveUserMovieHistoryRequest(
        @NotNull(message = "O ID do TMDB é obrigatório")
        @Positive(message = "O ID do TMDB deve ser positivo")
        Long tmdbMovieId,

        @NotNull(message = "O status é obrigatório")
        UserMovieStatus status,

        @PastOrPresent(message = "A data em que o filme foi assistido não pode estar no futuro")
        Instant watchedAt,

        @Min(value = 1, message = "A nota deve ser no mínimo 1")
        @Max(value = 5, message = "A nota deve ser no máximo 5")
        Integer userRating
) {

    @JsonIgnore
    @AssertTrue(message = "Data e nota do usuário só podem ser informadas para filmes assistidos")
    public boolean isWatchedMetadataConsistent() {
        return status == null
                || status == UserMovieStatus.WATCHED
                || (watchedAt == null && userRating == null);
    }
}
