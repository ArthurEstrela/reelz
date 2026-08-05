package com.roletadefilmes.social.api.dto;

import java.util.UUID;

public record SocialProviderResponse(
        UUID id,
        String name,
        String logoPath
) {
}
