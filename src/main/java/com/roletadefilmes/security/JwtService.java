package com.roletadefilmes.security;

import com.roletadefilmes.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Clock clock;
    private final Duration expiration;
    private final String issuer;
    private final SecretKey signingKey;
    private final JwtParser parser;

    public JwtService(
            Clock clock,
            @Value("${reelz.security.jwt.secret}") String base64Secret,
            @Value("${reelz.security.jwt.issuer}") String issuer,
            @Value("${reelz.security.jwt.expiration}") Duration expiration
    ) {
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.clock = clock;
        this.expiration = expiration;
        this.issuer = issuer;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .clock(() -> Date.from(Instant.now(clock)))
                .build();
    }

    public String generateToken(UUID userId) {
        return generateToken(userId, UserRole.USER);
    }

    public String generateToken(UUID userId, UserRole role) {
        return generateToken(userId, role, 0);
    }

    public String generateToken(UUID userId, UserRole role, long authVersion) {
        var issuedAt = Instant.now(clock);
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("role", role.name())
                .claim("ver", authVersion)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        var subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("JWT subject is required");
        }
        return UUID.fromString(subject);
    }

    public UserRole extractRole(String token) {
        var role = extractClaims(token).get("role", String.class);
        if (role == null || role.isBlank()) {
            return UserRole.USER;
        }
        return UserRole.valueOf(role);
    }

    public long extractAuthVersion(String token) {
        var version = extractClaims(token).get("ver", Long.class);
        return version == null ? 0 : version;
    }

    private Claims extractClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}
