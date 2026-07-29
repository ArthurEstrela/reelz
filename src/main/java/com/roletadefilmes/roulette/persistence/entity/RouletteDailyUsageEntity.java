package com.roletadefilmes.roulette.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDate;

@Entity
@Table(
        name = "roulette_daily_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roulette_daily_usage_user_date",
                columnNames = {"user_id", "usage_date"}
        )
)
public class RouletteDailyUsageEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "base_spins_used", nullable = false)
    private int baseSpinsUsed;

    @Column(name = "rewarded_spins_granted", nullable = false)
    private int rewardedSpinsGranted;

    @Column(name = "rewarded_spins_used", nullable = false)
    private int rewardedSpinsUsed;

    @Column(name = "timezone_snapshot", nullable = false, length = 50)
    private String timezoneSnapshot;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RouletteDailyUsageEntity() {
    }

    public RouletteDailyUsageEntity(UserAccountEntity user, LocalDate usageDate, String timezoneSnapshot) {
        this.user = user;
        this.usageDate = usageDate;
        this.timezoneSnapshot = timezoneSnapshot;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public int getBaseSpinsUsed() {
        return baseSpinsUsed;
    }

    public int getRewardedSpinsGranted() {
        return rewardedSpinsGranted;
    }

    public int getRewardedSpinsUsed() {
        return rewardedSpinsUsed;
    }

    public String getTimezoneSnapshot() {
        return timezoneSnapshot;
    }

    public long getVersion() {
        return version;
    }

    public int getRewardedSpinsRemaining() {
        return rewardedSpinsGranted - rewardedSpinsUsed;
    }

    public void consumeBaseSpin() {
        baseSpinsUsed++;
    }

    public void grantRewardedSpins(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Rewarded spins amount must be positive");
        }
        rewardedSpinsGranted += amount;
    }

    public void consumeRewardedSpin() {
        if (rewardedSpinsUsed >= rewardedSpinsGranted) {
            throw new IllegalStateException("No rewarded spins available");
        }
        rewardedSpinsUsed++;
    }
}
