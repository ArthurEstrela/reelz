package com.roletadefilmes.security;

import com.roletadefilmes.user.domain.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UserRole role) {

    public AuthenticatedUser(UUID userId) {
        this(userId, UserRole.USER);
    }
}
