package com.roletadefilmes.social.persistence.entity;

import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import com.roletadefilmes.social.domain.SocialRoomMemberRole;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "social_room_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_room_member",
                columnNames = {"room_id", "user_id"}
        )
)
public class SocialRoomMemberEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private SocialRoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private SocialRoomMemberRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected SocialRoomMemberEntity() {
    }

    public SocialRoomMemberEntity(
            SocialRoomEntity room,
            UserAccountEntity user,
            SocialRoomMemberRole role
    ) {
        this.room = room;
        this.user = user;
        this.role = role;
    }

    public SocialRoomEntity getRoom() {
        return room;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public SocialRoomMemberRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
