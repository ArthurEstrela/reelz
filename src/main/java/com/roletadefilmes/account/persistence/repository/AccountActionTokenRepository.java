package com.roletadefilmes.account.persistence.repository;

import com.roletadefilmes.account.domain.AccountActionTokenType;
import com.roletadefilmes.account.persistence.entity.AccountActionTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionTokenEntity, UUID> {

    @Query("""
            select token from AccountActionTokenEntity token
            join fetch token.user
            where token.tokenHash = :tokenHash and token.tokenType = :tokenType
            """)
    Optional<AccountActionTokenEntity> findForConsumption(
            @Param("tokenHash") String tokenHash,
            @Param("tokenType") AccountActionTokenType tokenType
    );

    boolean existsByUserIdAndTokenTypeAndConsumedAtIsNullAndCreatedAtAfter(
            UUID userId,
            AccountActionTokenType tokenType,
            Instant createdAfter
    );

    @Modifying
    @Query("""
            update AccountActionTokenEntity token
            set token.consumedAt = :consumedAt
            where token.user.id = :userId
              and token.tokenType = :tokenType
              and token.consumedAt is null
            """)
    int consumeOpenTokens(
            @Param("userId") UUID userId,
            @Param("tokenType") AccountActionTokenType tokenType,
            @Param("consumedAt") Instant consumedAt
    );

    void deleteByUserId(UUID userId);
}
