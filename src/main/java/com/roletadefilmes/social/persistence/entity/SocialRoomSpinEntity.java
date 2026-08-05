package com.roletadefilmes.social.persistence.entity;

import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Immutable
@Table(
        name = "social_room_spin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_room_spin_idempotency",
                        columnNames = {"room_id", "idempotency_key"}
                ),
                @UniqueConstraint(
                        name = "uk_social_room_spin_number",
                        columnNames = {"room_id", "spin_number"}
                )
        }
)
public class SocialRoomSpinEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private SocialRoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by_user_id", nullable = false)
    private UserAccountEntity triggeredBy;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "spin_number", nullable = false)
    private long spinNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> filters = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "movie_result", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> movieResult = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SocialRoomSpinEntity() {
    }

    public SocialRoomSpinEntity(
            SocialRoomEntity room,
            UserAccountEntity triggeredBy,
            String idempotencyKey,
            long spinNumber,
            Map<String, Object> filters,
            Map<String, Object> movieResult
    ) {
        this.room = room;
        this.triggeredBy = triggeredBy;
        this.idempotencyKey = idempotencyKey;
        this.spinNumber = spinNumber;
        this.filters = new LinkedHashMap<>(filters);
        this.movieResult = new LinkedHashMap<>(movieResult);
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public long getSpinNumber() {
        return spinNumber;
    }

    public Map<String, Object> getMovieResult() {
        return Map.copyOf(movieResult);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
