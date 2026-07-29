package com.roletadefilmes.user.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterUserRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptAValidRequest() {
        var request = new RegisterUserRequest(
                "Arthur",
                "arthur@example.com",
                "uma-senha-segura",
                "America/Sao_Paulo",
                "BR",
                true
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectInvalidRegistrationData() {
        var request = new RegisterUserRequest("A", "invalido", "123", "", "Brasil", false);

        assertThat(validator.validate(request)).hasSize(6);
    }
}
