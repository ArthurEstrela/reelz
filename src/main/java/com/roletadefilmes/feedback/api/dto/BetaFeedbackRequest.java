package com.roletadefilmes.feedback.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record BetaFeedbackRequest(
        @Min(1) @Max(5) int score,
        @Size(max = 1000) String message
) {
    public BetaFeedbackRequest {
        message = message == null || message.isBlank() ? null : message.trim();
    }
}
