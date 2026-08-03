package com.roletadefilmes.feedback.persistence.entity;

import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "beta_feedback")
public class BetaFeedbackEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Column(nullable = false)
    private short score;

    @Column(length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BetaFeedbackEntity() {
    }

    public BetaFeedbackEntity(UserAccountEntity user, int score, String message, Instant createdAt) {
        this.user = user;
        this.score = (short) score;
        this.message = message;
        this.createdAt = createdAt;
    }

    public int getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
