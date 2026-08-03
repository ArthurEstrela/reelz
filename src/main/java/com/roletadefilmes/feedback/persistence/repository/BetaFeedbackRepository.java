package com.roletadefilmes.feedback.persistence.repository;

import com.roletadefilmes.feedback.persistence.entity.BetaFeedbackEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BetaFeedbackRepository extends JpaRepository<BetaFeedbackEntity, UUID> {

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    @Query("""
            SELECT AVG(feedback.score)
              FROM BetaFeedbackEntity feedback
             WHERE feedback.createdAt >= :from
               AND feedback.createdAt < :to
            """)
    Double averageScore(Instant from, Instant to);

    List<BetaFeedbackEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Instant from,
            Instant to,
            Pageable pageable
    );
}
