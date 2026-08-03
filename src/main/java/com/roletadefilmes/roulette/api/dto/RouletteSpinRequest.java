package com.roletadefilmes.roulette.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record RouletteSpinRequest(
        @NotNull(message = "A chave de idempotência é obrigatória")
        UUID idempotencyKey,

        @NotEmpty(message = "Selecione pelo menos um serviço de streaming")
        @Size(max = 20, message = "Selecione no máximo 20 serviços de streaming")
        Set<UUID> providerIds,

        @Positive(message = "O gênero deve possuir um ID positivo")
        Integer genreId,

        UUID vibeId,

        UUID sessionId
) {

    public RouletteSpinRequest(
            UUID idempotencyKey,
            Set<UUID> providerIds,
            Integer genreId,
            UUID vibeId
    ) {
        this(idempotencyKey, providerIds, genreId, vibeId, null);
    }
}
