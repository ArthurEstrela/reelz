package com.roletadefilmes.achievement.api.dto;

import java.util.List;

public record AchievementOverviewResponse(
        int unlockedCount,
        int totalCount,
        List<AchievementResponse> achievements
) {
}
