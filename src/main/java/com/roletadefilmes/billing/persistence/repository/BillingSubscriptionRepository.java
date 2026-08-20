package com.roletadefilmes.billing.persistence.repository;

import com.roletadefilmes.billing.domain.BillingProvider;
import com.roletadefilmes.billing.domain.BillingSubscriptionStatus;
import com.roletadefilmes.billing.persistence.entity.BillingSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscriptionEntity, UUID> {

    Optional<BillingSubscriptionEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<BillingSubscriptionEntity> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            UUID userId,
            Collection<BillingSubscriptionStatus> statuses
    );

    Optional<BillingSubscriptionEntity> findByProviderAndProviderCheckoutId(
            BillingProvider provider,
            String providerCheckoutId
    );

    Optional<BillingSubscriptionEntity> findByProviderAndProviderSubscriptionId(
            BillingProvider provider,
            String providerSubscriptionId
    );

    boolean existsByUserIdAndStatusAndIdNot(
            UUID userId,
            BillingSubscriptionStatus status,
            UUID subscriptionId
    );

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<BillingSubscriptionStatus> statuses);
}
