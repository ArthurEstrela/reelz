package com.roletadefilmes.user.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteUserRequest(
        @NotBlank @Size(max = 128) String password,
        @AssertTrue(message = "Confirme a exclusao permanente da conta.") boolean confirmed
) {
}
