package com.roletadefilmes.user.persistence.repository;

import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    Optional<UserAccountEntity> findByEmailIgnoreCase(String email);

    Optional<UserAccountEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<UserAccountEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM UserAccountEntity user WHERE user.id = :userId AND user.deletedAt IS NULL")
    Optional<UserAccountEntity> findByIdForUpdate(@Param("userId") UUID userId);
}
