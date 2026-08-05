package com.roletadefilmes.social.api.dto;

import com.roletadefilmes.roulette.api.dto.RouletteMovieResponse;
import com.roletadefilmes.social.domain.SocialRoomStatus;
import com.roletadefilmes.social.domain.SocialRoomType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SocialRoomResponse(
        UUID id,
        SocialRoomType type,
        SocialRoomStatus status,
        String inviteCode,
        UUID hostUserId,
        String hostDisplayName,
        boolean currentUserHost,
        int capacity,
        List<SocialRoomMemberResponse> members,
        List<SocialProviderResponse> commonProviders,
        RouletteMovieResponse lastMovie,
        long lastSpinNumber,
        Instant updatedAt
) {
}
