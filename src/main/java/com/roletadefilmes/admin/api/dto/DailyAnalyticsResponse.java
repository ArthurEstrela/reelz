package com.roletadefilmes.admin.api.dto;

import java.time.LocalDate;

public record DailyAnalyticsResponse(
        LocalDate date,
        long registrations,
        long successfulSpins,
        long decisions
) {
}
