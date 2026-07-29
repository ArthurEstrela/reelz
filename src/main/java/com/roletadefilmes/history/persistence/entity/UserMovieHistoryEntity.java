package com.roletadefilmes.history.persistence.entity;

import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
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

import java.time.Instant;

@Entity
@Table(
        name = "user_movie_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_movie_history_user_movie",
                columnNames = {"user_id", "movie_id"}
        )
)
public class UserMovieHistoryEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private MovieCacheEntity movie;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserMovieStatus status;

    @Column(name = "watched_at")
    private Instant watchedAt;

    @Column(name = "user_rating")
    private Short userRating;

    protected UserMovieHistoryEntity() {
    }

    public UserMovieHistoryEntity(
            UserAccountEntity user,
            MovieCacheEntity movie,
            UserMovieStatus status,
            Instant watchedAt,
            Integer userRating
    ) {
        this.user = user;
        this.movie = movie;
        this.status = status;
        this.watchedAt = watchedAt;
        this.userRating = toSmallInt(userRating);
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public MovieCacheEntity getMovie() {
        return movie;
    }

    public UserMovieStatus getStatus() {
        return status;
    }

    public Instant getWatchedAt() {
        return watchedAt;
    }

    public Integer getUserRating() {
        return userRating == null ? null : userRating.intValue();
    }

    public void markAsWatched(Instant watchedAt, Integer userRating) {
        this.status = UserMovieStatus.WATCHED;
        this.watchedAt = watchedAt;
        this.userRating = toSmallInt(userRating);
    }

    public void moveToWatchlist() {
        this.status = UserMovieStatus.WATCHLIST;
        this.watchedAt = null;
        this.userRating = null;
    }

    private static Short toSmallInt(Integer rating) {
        if (rating == null) {
            return null;
        }
        if (rating < Short.MIN_VALUE || rating > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Rating does not fit in a PostgreSQL SMALLINT");
        }
        return rating.shortValue();
    }
}
