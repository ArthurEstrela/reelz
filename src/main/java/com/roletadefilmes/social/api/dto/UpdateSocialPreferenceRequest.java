package com.roletadefilmes.social.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UpdateSocialPreferenceRequest(
        @NotNull
        @Size(max = 3, message = "Escolha no máximo 3 gêneros")
        Set<@Positive(message = "O gênero deve possuir um ID positivo") Integer> genreIds,
        UUID vibeId,
        boolean ready
) {
}
