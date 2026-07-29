package com.roletadefilmes.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String OTHER_SECRET =
            "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final Instant NOW = Instant.parse("2026-07-29T15:00:00Z");

    @Test
    void shouldGenerateAndExtractTheUserId() {
        var service = service(SECRET, NOW, Duration.ofHours(2));
        var userId = UUID.randomUUID();

        var token = service.generateToken(userId);

        assertThat(service.extractUserId(token)).isEqualTo(userId);
        assertThat(service.getExpirationSeconds()).isEqualTo(7_200);
    }

    @Test
    void shouldRejectATokenSignedWithAnotherSecret() {
        var token = service(SECRET, NOW, Duration.ofHours(2)).generateToken(UUID.randomUUID());
        var verifier = service(OTHER_SECRET, NOW, Duration.ofHours(2));

        assertThatThrownBy(() -> verifier.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectAnExpiredToken() {
        var token = service(SECRET, NOW, Duration.ofMinutes(30)).generateToken(UUID.randomUUID());
        var verifier = service(SECRET, NOW.plus(Duration.ofHours(1)), Duration.ofMinutes(30));

        assertThatThrownBy(() -> verifier.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    private JwtService service(String secret, Instant now, Duration expiration) {
        return new JwtService(
                Clock.fixed(now, ZoneOffset.UTC),
                secret,
                "reelz-api",
                expiration
        );
    }
}
