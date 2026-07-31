package com.roletadefilmes.auth.api.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        boolean onboardingCompleted
) {
}
