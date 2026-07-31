package com.roletadefilmes.streaming.persistence.repository;

import com.roletadefilmes.streaming.persistence.entity.UserStreamingPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserStreamingPreferenceRepository
        extends JpaRepository<UserStreamingPreferenceEntity, UUID> {

    List<UserStreamingPreferenceEntity> findAllByUserId(UUID userId);

    @Query("""
            SELECT preference
              FROM UserStreamingPreferenceEntity preference
              JOIN FETCH preference.provider provider
             WHERE preference.user.id = :userId
             ORDER BY provider.displayPriority ASC, provider.name ASC
            """)
    List<UserStreamingPreferenceEntity> findAllWithProviderByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndProviderId(UUID userId, UUID providerId);
}
