package com.roletadefilmes.social.persistence.repository;

import com.roletadefilmes.social.persistence.entity.SocialRoomSpinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SocialRoomSpinRepository extends JpaRepository<SocialRoomSpinEntity, UUID> {

    Optional<SocialRoomSpinEntity> findFirstByRoomIdOrderBySpinNumberDesc(UUID roomId);

    Optional<SocialRoomSpinEntity> findByRoomIdAndIdempotencyKey(UUID roomId, String idempotencyKey);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO social_room_spin_participant (
                id, spin_id, user_id, room_type, participant_count, created_at
            )
            SELECT gen_random_uuid(), :spinId, member.user_id, room.room_type, :participantCount,
                   CURRENT_TIMESTAMP
              FROM social_room_member member
              JOIN social_room room ON room.id = member.room_id
             WHERE member.room_id = :roomId
            ON CONFLICT (spin_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int snapshotParticipants(
            @Param("spinId") UUID spinId,
            @Param("roomId") UUID roomId,
            @Param("participantCount") int participantCount
    );
}
