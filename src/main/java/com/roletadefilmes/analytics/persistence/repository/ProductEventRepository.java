package com.roletadefilmes.analytics.persistence.repository;

import com.roletadefilmes.analytics.persistence.entity.ProductEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProductEventRepository extends JpaRepository<ProductEventEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO product_event (
                id, event_id, user_id, session_id, event_type, properties, occurred_at
            ) VALUES (
                :id, :eventId, :userId, :sessionId, :eventType, CAST(:properties AS jsonb), :occurredAt
            )
            ON CONFLICT (user_id, event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("userId") UUID userId,
            @Param("sessionId") UUID sessionId,
            @Param("eventType") String eventType,
            @Param("properties") String properties,
            @Param("occurredAt") Instant occurredAt
    );

    long deleteByOccurredAtBefore(Instant cutoff);
}
