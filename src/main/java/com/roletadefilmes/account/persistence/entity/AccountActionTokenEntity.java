package com.roletadefilmes.account.persistence.entity;

import com.roletadefilmes.account.domain.AccountActionTokenType;
import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "account_action_token")
public class AccountActionTokenEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 30)
    private AccountActionTokenType tokenType;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected AccountActionTokenEntity() {
    }

    public AccountActionTokenEntity(
            UserAccountEntity user,
            AccountActionTokenType tokenType,
            String tokenHash,
            Instant expiresAt
    ) {
        this.user = user;
        this.tokenType = tokenType;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public AccountActionTokenType getTokenType() {
        return tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public boolean canBeConsumedAt(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now) && user.getDeletedAt() == null;
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }
}
