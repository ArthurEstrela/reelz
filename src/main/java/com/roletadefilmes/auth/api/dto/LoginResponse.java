package com.roletadefilmes.auth.api.dto;

import com.roletadefilmes.user.domain.UserRole;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        boolean onboardingCompleted,
        UserRole role
) {
}
