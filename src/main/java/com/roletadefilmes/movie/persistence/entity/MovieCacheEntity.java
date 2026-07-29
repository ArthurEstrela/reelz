package com.roletadefilmes.movie.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "movie_cache")
public class MovieCacheEntity extends AuditableUuidEntity {

    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Long tmdbId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @Column(name = "poster_path", length = 255)
    private String posterPath;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "vote_average", precision = 3, scale = 1)
    private BigDecimal voteAverage;

    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "genre_ids", nullable = false, columnDefinition = "integer[]")
    private Integer[] genreIds = new Integer[0];

    @Column(name = "adult", nullable = false)
    private boolean adult;

    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "tmdb_last_synced_at", nullable = false)
    private Instant tmdbLastSyncedAt;

    protected MovieCacheEntity() {
    }

    public MovieCacheEntity(Long tmdbId, String title, Integer[] genreIds, Instant tmdbLastSyncedAt) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.genreIds = genreIds == null ? new Integer[0] : genreIds.clone();
        this.tmdbLastSyncedAt = tmdbLastSyncedAt;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getOverview() {
        return overview;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public BigDecimal getVoteAverage() {
        return voteAverage;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public Integer[] getGenreIds() {
        return genreIds.clone();
    }

    public boolean isAdult() {
        return adult;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public Instant getTmdbLastSyncedAt() {
        return tmdbLastSyncedAt;
    }

    public void refreshMetadata(
            String title,
            String originalTitle,
            String overview,
            String posterPath,
            LocalDate releaseDate,
            BigDecimal voteAverage,
            int voteCount,
            Integer[] genreIds,
            boolean adult,
            String originalLanguage,
            Integer runtimeMinutes,
            Instant syncedAt
    ) {
        this.title = title;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.posterPath = posterPath;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
        this.genreIds = genreIds == null ? new Integer[0] : genreIds.clone();
        this.adult = adult;
        this.originalLanguage = originalLanguage;
        this.runtimeMinutes = runtimeMinutes;
        this.tmdbLastSyncedAt = syncedAt;
    }
}
