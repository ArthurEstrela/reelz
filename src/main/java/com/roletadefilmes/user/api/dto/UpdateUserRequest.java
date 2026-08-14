package com.roletadefilmes.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @NotBlank @Size(max = 50) String timezone,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode
) {
}
