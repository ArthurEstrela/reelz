package com.roletadefilmes.onboarding.api.dto;

public record CompleteOnboardingResponse(
        boolean onboardingCompleted,
        int watchedMoviesAdded
) {
}
