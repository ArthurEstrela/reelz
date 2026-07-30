package com.roletadefilmes.catalog.api.dto;

import java.util.UUID;

public record VibeCatalogResponse(
        UUID id,
        String name
) {
}
