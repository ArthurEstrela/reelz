package com.roletadefilmes.billing.persistence.repository;

import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.persistence.entity.PaymentWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEventEntity, UUID> {
    boolean existsByProviderAndProviderEventId(BillingProvider provider, String providerEventId);
}
