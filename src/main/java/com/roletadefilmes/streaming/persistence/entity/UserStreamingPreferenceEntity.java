package com.roletadefilmes.streaming.persistence.entity;

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

import java.time.Instant;

@Entity
@Table(
        name = "user_streaming_preference",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_streaming_preference",
                columnNames = {"user_id", "provider_id"}
        )
)
public class UserStreamingPreferenceEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private StreamingProviderEntity provider;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserStreamingPreferenceEntity() {
    }

    public UserStreamingPreferenceEntity(UserAccountEntity user, StreamingProviderEntity provider) {
        this.user = user;
        this.provider = provider;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public StreamingProviderEntity getProvider() {
        return provider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
