package com.roletadefilmes.social.persistence.repository;

import com.roletadefilmes.social.persistence.entity.SocialRoomEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SocialRoomRepository extends JpaRepository<SocialRoomEntity, UUID> {

    boolean existsByInviteCode(String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT room
              FROM SocialRoomEntity room
              JOIN FETCH room.owner
             WHERE room.id = :roomId
            """)
    Optional<SocialRoomEntity> findByIdForUpdate(@Param("roomId") UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT room
              FROM SocialRoomEntity room
              JOIN FETCH room.owner
             WHERE room.inviteCode = :inviteCode
            """)
    Optional<SocialRoomEntity> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

    @Query("""
            SELECT DISTINCT room
              FROM SocialRoomEntity room
              JOIN FETCH room.owner
              JOIN SocialRoomMemberEntity member ON member.room = room
             WHERE member.user.id = :userId
             ORDER BY room.updatedAt DESC
            """)
    List<SocialRoomEntity> findAllForUser(@Param("userId") UUID userId);
}
