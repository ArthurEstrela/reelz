package com.roletadefilmes.history.api.dto;

import com.roletadefilmes.history.domain.UserMovieStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SaveUserMovieHistoryRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptWatchedMetadataForWatchedMovie() {
        var request = new SaveUserMovieHistoryRequest(
                550L,
                UserMovieStatus.WATCHED,
                Instant.now(),
                5
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectWatchedMetadataForWatchlistMovie() {
        var request = new SaveUserMovieHistoryRequest(
                550L,
                UserMovieStatus.WATCHLIST,
                Instant.now(),
                5
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("watchedMetadataConsistent");
    }
}
