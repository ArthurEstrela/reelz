package com.roletadefilmes.social.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.social.domain.SocialRoomStatus;
import com.roletadefilmes.social.domain.SocialRoomType;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "social_room")
public class SocialRoomEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserAccountEntity owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private SocialRoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialRoomStatus status = SocialRoomStatus.OPEN;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Column(name = "spin_sequence", nullable = false)
    private long spinSequence;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SocialRoomEntity() {
    }

    public SocialRoomEntity(UserAccountEntity owner, SocialRoomType roomType, String inviteCode) {
        this.owner = owner;
        this.roomType = roomType;
        this.inviteCode = inviteCode;
    }

    public UserAccountEntity getOwner() {
        return owner;
    }

    public SocialRoomType getRoomType() {
        return roomType;
    }

    public SocialRoomStatus getStatus() {
        return status;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public long getSpinSequence() {
        return spinSequence;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public boolean isOpen() {
        return status == SocialRoomStatus.OPEN;
    }

    public long nextSpinNumber() {
        if (!isOpen()) {
            throw new IllegalStateException("Cannot spin a closed room");
        }
        return ++spinSequence;
    }

    public void close(Instant instant) {
        status = SocialRoomStatus.CLOSED;
        closedAt = instant;
    }
}
