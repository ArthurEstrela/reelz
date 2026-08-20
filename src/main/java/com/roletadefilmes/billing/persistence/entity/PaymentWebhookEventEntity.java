package com.roletadefilmes.billing.persistence.entity;

import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.WebhookProcessingStatus;
import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_event")
public class PaymentWebhookEventEntity extends AuditableUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private BillingProvider provider;

    @Column(name = "provider_event_id", nullable = false, length = 160)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private WebhookProcessingStatus processingStatus;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected PaymentWebhookEventEntity() {
    }

    public PaymentWebhookEventEntity(String providerEventId, String eventType, String payloadSha256, Instant receivedAt) {
        this.provider = BillingProvider.ABACATEPAY;
        this.providerEventId = providerEventId;
        this.eventType = eventType;
        this.payloadSha256 = payloadSha256;
        this.processingStatus = WebhookProcessingStatus.RECEIVED;
        this.receivedAt = receivedAt;
    }

    public WebhookProcessingStatus getProcessingStatus() { return processingStatus; }

    public void processed(Instant instant) {
        this.processingStatus = WebhookProcessingStatus.PROCESSED;
        this.processedAt = instant;
    }

    public void ignored(Instant instant) {
        this.processingStatus = WebhookProcessingStatus.IGNORED;
        this.processedAt = instant;
    }
}
