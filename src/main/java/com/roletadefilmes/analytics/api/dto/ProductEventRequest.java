package com.roletadefilmes.analytics.api.dto;

import com.roletadefilmes.analytics.domain.ProductEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ProductEventRequest(
        @NotNull UUID eventId,
        @NotNull UUID sessionId,
        @NotNull ProductEventType eventType,
        @Positive Long movieId,
        UUID providerId
) {
}
