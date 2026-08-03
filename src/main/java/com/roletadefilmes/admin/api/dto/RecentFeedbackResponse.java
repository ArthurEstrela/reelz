package com.roletadefilmes.admin.api.dto;

import java.time.Instant;

public record RecentFeedbackResponse(
        Instant createdAt,
        int score,
        String message
) {
}
