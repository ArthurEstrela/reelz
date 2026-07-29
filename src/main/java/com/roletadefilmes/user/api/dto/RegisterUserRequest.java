package com.roletadefilmes.user.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
        String displayName,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres")
        String password,

        @NotBlank(message = "O fuso horário é obrigatório")
        @Size(max = 50, message = "O fuso horário deve ter no máximo 50 caracteres")
        String timezone,

        @NotBlank(message = "O país é obrigatório")
        @Pattern(regexp = "^[A-Z]{2}$", message = "O país deve usar o código ISO 3166-1 alpha-2, como BR")
        String countryCode,

        @AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade")
        boolean termsAccepted
) {
}
