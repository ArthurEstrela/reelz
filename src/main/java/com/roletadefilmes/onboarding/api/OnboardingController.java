package com.roletadefilmes.onboarding.api;

import com.roletadefilmes.onboarding.api.dto.CompleteOnboardingRequest;
import com.roletadefilmes.onboarding.api.dto.CompleteOnboardingResponse;
import com.roletadefilmes.onboarding.api.dto.OnboardingMoviesResponse;
import com.roletadefilmes.onboarding.service.OnboardingService;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/movies")
    public ResponseEntity<OnboardingMoviesResponse> movies(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "25") @Min(20) @Max(30) int limit
    ) {
        return ResponseEntity.ok(onboardingService.getMovies(authenticatedUser.userId(), limit));
    }

    @PostMapping("/complete")
    public ResponseEntity<CompleteOnboardingResponse> complete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        return ResponseEntity.ok(onboardingService.complete(authenticatedUser.userId(), request));
    }
}
