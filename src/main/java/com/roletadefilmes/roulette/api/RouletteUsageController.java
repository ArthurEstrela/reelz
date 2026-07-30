package com.roletadefilmes.roulette.api;

import com.roletadefilmes.roulette.api.dto.SpinQuotaResponse;
import com.roletadefilmes.roulette.service.RouletteUsageService;
import com.roletadefilmes.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roulette/usage")
public class RouletteUsageController {

    private final RouletteUsageService usageService;

    public RouletteUsageController(RouletteUsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping("/today")
    public ResponseEntity<SpinQuotaResponse> getToday(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(usageService.getToday(authenticatedUser.userId()));
    }
}
