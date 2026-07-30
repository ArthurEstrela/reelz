package com.roletadefilmes.catalog.api.dto;

import java.util.UUID;

public record ProviderCatalogResponse(
        UUID id,
        String name
) {
}
