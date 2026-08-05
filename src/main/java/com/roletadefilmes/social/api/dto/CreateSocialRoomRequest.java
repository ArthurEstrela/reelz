package com.roletadefilmes.social.api.dto;

import com.roletadefilmes.social.domain.SocialRoomType;
import jakarta.validation.constraints.NotNull;

public record CreateSocialRoomRequest(
        @NotNull SocialRoomType type
) {
}
