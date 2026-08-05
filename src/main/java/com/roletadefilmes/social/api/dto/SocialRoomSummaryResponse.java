package com.roletadefilmes.social.api.dto;

import com.roletadefilmes.social.domain.SocialRoomStatus;
import com.roletadefilmes.social.domain.SocialRoomType;

import java.time.Instant;
import java.util.UUID;

public record SocialRoomSummaryResponse(
        UUID id,
        SocialRoomType type,
        SocialRoomStatus status,
        boolean currentUserHost,
        int memberCount,
        int capacity,
        long lastSpinNumber,
        Instant updatedAt
) {
}
