package com.roletadefilmes.onboarding.api.dto;

import java.math.BigDecimal;

public record OnboardingMovieResponse(
        Long movieId,
        String title,
        String posterPath,
        BigDecimal voteAverage
) {
}
