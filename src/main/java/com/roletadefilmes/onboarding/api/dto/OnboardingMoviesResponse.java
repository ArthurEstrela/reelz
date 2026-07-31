package com.roletadefilmes.onboarding.api.dto;

import java.util.List;

public record OnboardingMoviesResponse(
        List<OnboardingMovieResponse> movies,
        int targetCount
) {
}
