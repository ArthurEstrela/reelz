package com.roletadefilmes.roulette.api.dto;

public record SpinQuotaResponse(
        boolean unlimited,
        Integer dailyLimit,
        Integer remainingDailySpins,
        int remainingRewardedSpins
) {
}
