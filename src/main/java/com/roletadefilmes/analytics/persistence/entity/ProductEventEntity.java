package com.roletadefilmes.analytics.persistence.entity;

import com.roletadefilmes.analytics.domain.ProductEventType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "product_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_event_user_event",
                columnNames = {"user_id", "event_id"}
        )
)
public class ProductEventEntity extends AbstractUuidEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ProductEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> properties = new HashMap<>();

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ProductEventEntity() {
    }

    public ProductEventEntity(
            UUID eventId,
            UserAccountEntity user,
            UUID sessionId,
            ProductEventType eventType,
            Map<String, Object> properties,
            Instant occurredAt
    ) {
        this.eventId = eventId;
        this.user = user;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.properties = new HashMap<>(properties);
        this.occurredAt = occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public ProductEventType getEventType() {
        return eventType;
    }

    public Map<String, Object> getProperties() {
        return Map.copyOf(properties);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
