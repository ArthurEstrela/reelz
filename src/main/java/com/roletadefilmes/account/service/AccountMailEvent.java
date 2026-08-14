package com.roletadefilmes.account.service;

import com.roletadefilmes.account.domain.AccountActionTokenType;

public record AccountMailEvent(
        String email,
        String displayName,
        String rawToken,
        AccountActionTokenType tokenType
) {
}
