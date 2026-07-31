package com.roletadefilmes.streaming.api.dto;

import java.util.List;
import java.util.UUID;

public record StreamingPreferencesResponse(
        List<UUID> providerIds
) {
    public StreamingPreferencesResponse {
        providerIds = List.copyOf(providerIds);
    }
}
