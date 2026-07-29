package com.roletadefilmes.reward.persistence.entity;

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
        name = "reward_grant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reward_grant_external",
                columnNames = {"ad_provider", "external_reward_id"}
        )
)
public class RewardGrantEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Column(name = "ad_provider", nullable = false, length = 80)
    private String adProvider;

    @Column(name = "external_reward_id", nullable = false, length = 255)
    private String externalRewardId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    protected RewardGrantEntity() {
    }

    public RewardGrantEntity(
            UserAccountEntity user,
            String adProvider,
            String externalRewardId,
            int amount
    ) {
        this.user = user;
        this.adProvider = adProvider;
        this.externalRewardId = externalRewardId;
        this.amount = amount;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public String getAdProvider() {
        return adProvider;
    }

    public String getExternalRewardId() {
        return externalRewardId;
    }

    public int getAmount() {
        return amount;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }
}
