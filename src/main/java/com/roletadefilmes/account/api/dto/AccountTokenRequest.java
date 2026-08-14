package com.roletadefilmes.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountTokenRequest(
        @NotBlank @Size(max = 256) String token
) {
}
