package com.roletadefilmes.social.persistence.entity;

import com.roletadefilmes.shared.persistence.AbstractUuidEntity;
import com.roletadefilmes.social.domain.SocialRoomMemberRole;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.vibe.persistence.entity.VibeEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "selected_genre_ids", nullable = false, columnDefinition = "integer[]")
    private Integer[] selectedGenreIds = new Integer[0];

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_vibe_id")
    private VibeEntity selectedVibe;

    @Column(nullable = false)
    private boolean ready;

    @Column(name = "preference_updated_at")
    private Instant preferenceUpdatedAt;

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

    public Integer[] getSelectedGenreIds() {
        return selectedGenreIds.clone();
    }

    public VibeEntity getSelectedVibe() {
        return selectedVibe;
    }

    public boolean isReady() {
        return ready;
    }

    public Instant getPreferenceUpdatedAt() {
        return preferenceUpdatedAt;
    }

    public void updatePreferences(
            Integer[] genreIds,
            VibeEntity vibe,
            boolean ready,
            Instant updatedAt
    ) {
        this.selectedGenreIds = genreIds == null ? new Integer[0] : genreIds.clone();
        this.selectedVibe = vibe;
        this.ready = ready;
        this.preferenceUpdatedAt = updatedAt;
    }

    public void resetReady() {
        this.ready = false;
    }
}
