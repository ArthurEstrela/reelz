package com.roletadefilmes.streaming.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UpdateStreamingPreferencesRequest(
        @NotNull(message = "A lista de provedores é obrigatória")
        @Size(max = 20, message = "Selecione no máximo 20 provedores")
        Set<@NotNull(message = "O ID do provedor é obrigatório") UUID> providerIds
) {
}
