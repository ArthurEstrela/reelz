package com.roletadefilmes.roulette.persistence.entity;

import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.roulette.domain.RouletteSpinStatus;
import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
        name = "roulette_spin",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roulette_spin_idempotency",
                columnNames = {"user_id", "idempotency_key"}
        )
)
public class RouletteSpinEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> filters = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private MovieCacheEntity movie;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RouletteSpinStatus status = RouletteSpinStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected RouletteSpinEntity() {
    }

    public RouletteSpinEntity(UserAccountEntity user, String idempotencyKey, Map<String, Object> filters) {
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        if (filters != null) {
            this.filters = new HashMap<>(filters);
        }
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getFilters() {
        return Map.copyOf(filters);
    }

    public MovieCacheEntity getMovie() {
        return movie;
    }

    public RouletteSpinStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void succeedWith(MovieCacheEntity movie, Instant completedAt) {
        this.movie = movie;
        this.status = RouletteSpinStatus.SUCCEEDED;
        this.completedAt = completedAt;
        this.failureReason = null;
    }

    public void finishWithoutCandidate(Instant completedAt) {
        this.status = RouletteSpinStatus.NO_CANDIDATE;
        this.completedAt = completedAt;
    }

    public void fail(String reason, Instant completedAt) {
        this.status = RouletteSpinStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = completedAt;
    }
}
