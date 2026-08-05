package com.roletadefilmes.social.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinSocialRoomRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{8}$", message = "O convite deve possuir 8 caracteres")
        String inviteCode
) {
}
