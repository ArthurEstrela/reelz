package com.roletadefilmes.social.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SocialRoomMemberResponse(
        UUID userId,
        String displayName,
        boolean host,
        Instant joinedAt,
        List<SocialProviderResponse> providers,
        List<Integer> selectedGenreIds,
        UUID selectedVibeId,
        String selectedVibeName,
        boolean ready,
        Instant preferenceUpdatedAt
) {
}
