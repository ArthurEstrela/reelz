package com.roletadefilmes.onboarding.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CompleteOnboardingRequest(
        @NotEmpty
        @Size(max = 30)
        Set<@NotNull @Positive Long> presentedMovieIds,

        @NotNull
        @Size(max = 30)
        Set<@NotNull @Positive Long> watchedMovieIds
) {
}
