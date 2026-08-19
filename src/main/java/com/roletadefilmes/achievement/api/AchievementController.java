package com.roletadefilmes.achievement.api;

import com.roletadefilmes.achievement.api.dto.AchievementOverviewResponse;
import com.roletadefilmes.achievement.service.AchievementService;
import com.roletadefilmes.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ResponseEntity<AchievementOverviewResponse> getOverview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(achievementService.getOverview(authenticatedUser.userId()));
    }
}
