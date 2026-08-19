package com.roletadefilmes.achievement.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "user_achievement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_achievement_user_definition",
                columnNames = {"user_id", "achievement_id"}
        )
)
public class UserAchievementEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id", nullable = false)
    private AchievementDefinitionEntity achievement;

    @Column(name = "progress_value", nullable = false)
    private long progressValue;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    protected UserAchievementEntity() {
    }

    public UserAchievementEntity(
            UserAccountEntity user,
            AchievementDefinitionEntity achievement,
            long progressValue,
            Instant unlockedAt
    ) {
        this.user = user;
        this.achievement = achievement;
        this.progressValue = progressValue;
        this.unlockedAt = unlockedAt;
    }

    public AchievementDefinitionEntity getAchievement() {
        return achievement;
    }

    public long getProgressValue() {
        return progressValue;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public void refresh(long progress, Instant evaluatedAt) {
        progressValue = Math.max(progressValue, progress);
        if (unlockedAt == null && progressValue >= achievement.getTargetValue()) {
            unlockedAt = evaluatedAt;
        }
    }
}
