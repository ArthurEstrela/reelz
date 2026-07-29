package com.roletadefilmes.streaming.persistence.repository;

import com.roletadefilmes.streaming.persistence.entity.UserStreamingPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserStreamingPreferenceRepository
        extends JpaRepository<UserStreamingPreferenceEntity, UUID> {

    List<UserStreamingPreferenceEntity> findAllByUserId(UUID userId);

    boolean existsByUserIdAndProviderId(UUID userId, UUID providerId);
}
