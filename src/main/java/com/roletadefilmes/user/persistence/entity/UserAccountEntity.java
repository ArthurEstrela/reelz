package com.roletadefilmes.user.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.user.domain.PlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "user_account")
public class UserAccountEntity extends AuditableUuidEntity {

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private PlanType plan = PlanType.FREE;

    @Column(name = "premium_until")
    private Instant premiumUntil;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserAccountEntity() {
    }

    public UserAccountEntity(
            String email,
            String passwordHash,
            String displayName,
            String timezone,
            String countryCode
    ) {
        this.email = email.toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timezone = timezone;
        this.countryCode = countryCode.toUpperCase(Locale.ROOT);
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PlanType getPlan() {
        return plan;
    }

    public Instant getPremiumUntil() {
        return premiumUntil;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getVersion() {
        return version;
    }

    public boolean isPremiumAt(Instant instant) {
        return plan == PlanType.PREMIUM
                && (premiumUntil == null || premiumUntil.isAfter(instant));
    }

    public void markEmailVerified(Instant verifiedAt) {
        this.emailVerifiedAt = verifiedAt;
    }

    public void completeOnboarding(Instant completedAt) {
        this.onboardingCompletedAt = completedAt;
    }

    public void activatePremium(Instant validUntil) {
        this.plan = PlanType.PREMIUM;
        this.premiumUntil = validUntil;
    }
}
