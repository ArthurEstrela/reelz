package com.roletadefilmes.social.persistence.repository;

import com.roletadefilmes.social.persistence.entity.SocialRoomSpinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialRoomSpinRepository extends JpaRepository<SocialRoomSpinEntity, UUID> {

    Optional<SocialRoomSpinEntity> findFirstByRoomIdOrderBySpinNumberDesc(UUID roomId);

    Optional<SocialRoomSpinEntity> findByRoomIdAndIdempotencyKey(UUID roomId, String idempotencyKey);
}
