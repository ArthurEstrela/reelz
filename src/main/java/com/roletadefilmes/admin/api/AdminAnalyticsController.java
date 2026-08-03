package com.roletadefilmes.admin.api;

import com.roletadefilmes.admin.api.dto.AnalyticsOverviewResponse;
import com.roletadefilmes.admin.service.AdminAnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public AnalyticsOverviewResponse overview(
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days
    ) {
        return analyticsService.overview(days);
    }
}
