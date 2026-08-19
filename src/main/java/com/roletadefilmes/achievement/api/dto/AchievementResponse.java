package com.roletadefilmes.achievement.api.dto;

import com.roletadefilmes.achievement.domain.AchievementCategory;
import com.roletadefilmes.achievement.domain.AchievementCode;

import java.time.Instant;

public record AchievementResponse(
        AchievementCode code,
        String name,
        String description,
        String iconKey,
        AchievementCategory category,
        long target,
        long progress,
        boolean unlocked,
        Instant unlockedAt
) {
}
