package com.roletadefilmes.billing.persistence.entity;

import com.roletadefilmes.billing.domain.BillingPlanCode;
import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "billing_subscription")
public class BillingSubscriptionEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private BillingProvider provider;

    @Column(name = "provider_checkout_id", length = 120)
    private String providerCheckoutId;

    @Column(name = "provider_subscription_id", length = 120)
    private String providerSubscriptionId;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 40)
    private BillingPlanCode planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BillingSubscriptionStatus status;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected BillingSubscriptionEntity() {
    }

    public BillingSubscriptionEntity(UserAccountEntity user, BillingPlanCode planCode, int amountCents) {
        this.user = user;
        this.provider = BillingProvider.ABACATEPAY;
        this.planCode = planCode;
        this.status = BillingSubscriptionStatus.CHECKOUT_PENDING;
        this.amountCents = amountCents;
        this.currency = "BRL";
    }

    public UserAccountEntity getUser() { return user; }
    public BillingProvider getProvider() { return provider; }
    public String getProviderCheckoutId() { return providerCheckoutId; }
    public String getProviderSubscriptionId() { return providerSubscriptionId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public BillingPlanCode getPlanCode() { return planCode; }
    public BillingSubscriptionStatus getStatus() { return status; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getPaymentMethod() { return paymentMethod; }
    public Instant getCurrentPeriodStart() { return currentPeriodStart; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Instant getCanceledAt() { return canceledAt; }
    public long getVersion() { return version; }

    public void attachCheckout(String providerCheckoutId, String checkoutUrl) {
        this.providerCheckoutId = providerCheckoutId;
        this.checkoutUrl = checkoutUrl;
    }

    public void activate(String providerSubscriptionId, String paymentMethod, Instant start, Instant end) {
        this.providerSubscriptionId = providerSubscriptionId;
        this.paymentMethod = paymentMethod;
        this.status = BillingSubscriptionStatus.ACTIVE;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
        this.canceledAt = null;
        this.checkoutUrl = null;
    }

    public void renew(String paymentMethod, Instant start, Instant end) {
        this.paymentMethod = paymentMethod;
        this.status = BillingSubscriptionStatus.ACTIVE;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
    }

    public void markPastDue() {
        this.status = BillingSubscriptionStatus.PAST_DUE;
    }

    public void cancel(Instant canceledAt) {
        this.status = BillingSubscriptionStatus.CANCELED;
        this.canceledAt = canceledAt;
        this.checkoutUrl = null;
    }
}
