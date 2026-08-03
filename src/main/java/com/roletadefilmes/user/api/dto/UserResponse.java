package com.roletadefilmes.user.api.dto;

import com.roletadefilmes.user.domain.PlanType;
import com.roletadefilmes.user.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String displayName,
        String email,
        PlanType plan,
        UserRole role,
        String timezone,
        String countryCode,
        boolean onboardingCompleted,
        Instant createdAt
) {
}
