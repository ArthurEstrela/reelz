package com.roletadefilmes.roulette.service;

import com.roletadefilmes.roulette.api.dto.SpinQuotaResponse;
import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;

public final class RouletteQuotaPolicy {

    public static final int FREE_DAILY_SPIN_LIMIT = 5;

    private RouletteQuotaPolicy() {
    }

    public static SpinQuotaResponse toResponse(
            RouletteDailyUsageEntity usage,
            boolean premium
    ) {
        var remainingRewardedSpins = usage == null ? 0 : usage.getRewardedSpinsRemaining();
        if (premium) {
            return new SpinQuotaResponse(true, null, null, remainingRewardedSpins);
        }

        var baseSpinsUsed = usage == null ? 0 : usage.getBaseSpinsUsed();
        var remainingDailySpins = Math.max(0, FREE_DAILY_SPIN_LIMIT - baseSpinsUsed);
        return new SpinQuotaResponse(
                false,
                FREE_DAILY_SPIN_LIMIT,
                remainingDailySpins,
                remainingRewardedSpins
        );
    }
}
