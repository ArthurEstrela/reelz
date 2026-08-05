package com.roletadefilmes.social.persistence.repository;

import com.roletadefilmes.social.persistence.entity.SocialRoomMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SocialRoomMemberRepository extends JpaRepository<SocialRoomMemberEntity, UUID> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    long countByRoomId(UUID roomId);

    Optional<SocialRoomMemberEntity> findByRoomIdAndUserId(UUID roomId, UUID userId);

    @Query("""
            SELECT member
              FROM SocialRoomMemberEntity member
              JOIN FETCH member.user
              LEFT JOIN FETCH member.selectedVibe
             WHERE member.room.id = :roomId
             ORDER BY member.joinedAt, member.id
            """)
    List<SocialRoomMemberEntity> findAllWithUserByRoomId(@Param("roomId") UUID roomId);

    @Modifying
    @Query("""
            DELETE FROM SocialRoomMemberEntity member
             WHERE member.room.id = :roomId
               AND member.user.id = :userId
            """)
    int deleteMembership(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}
