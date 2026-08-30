package com.roletadefilmes.streaming.persistence.entity;

import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import com.roletadefilmes.streaming.domain.MonetizationType;
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
        name = "movie_streaming_offer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_movie_streaming_offer",
                columnNames = {"movie_id", "provider_id", "country_code", "monetization_type"}
        )
)
public class MovieStreamingOfferEntity extends AuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private MovieCacheEntity movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private StreamingProviderEntity provider;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "monetization_type", nullable = false, length = 20)
    private MonetizationType monetizationType;

    @Column(name = "attribution_url", length = 1000)
    private String attributionUrl;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "available_until")
    private Instant availableUntil;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @Column(name = "catalog_source", nullable = false, length = 30)
    private String catalogSource = "TMDB";

    protected MovieStreamingOfferEntity() {
    }

    public MovieStreamingOfferEntity(
            MovieCacheEntity movie,
            StreamingProviderEntity provider,
            String countryCode,
            MonetizationType monetizationType,
            Instant lastSyncedAt
    ) {
        this.movie = movie;
        this.provider = provider;
        this.countryCode = countryCode;
        this.monetizationType = monetizationType;
        this.lastSyncedAt = lastSyncedAt;
    }

    public MovieCacheEntity getMovie() {
        return movie;
    }

    public StreamingProviderEntity getProvider() {
        return provider;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public MonetizationType getMonetizationType() {
        return monetizationType;
    }

    public String getAttributionUrl() {
        return attributionUrl;
    }

    public Instant getAvailableFrom() {
        return availableFrom;
    }

    public Instant getAvailableUntil() {
        return availableUntil;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getCatalogSource() {
        return catalogSource;
    }

    public void refreshAvailability(
            String attributionUrl,
            Instant availableFrom,
            Instant availableUntil,
            Instant syncedAt
    ) {
        this.attributionUrl = attributionUrl;
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
        this.lastSyncedAt = syncedAt;
    }

    public void refreshAvailability(
            String attributionUrl,
            Instant availableFrom,
            Instant availableUntil,
            Instant syncedAt,
            String catalogSource
    ) {
        refreshAvailability(attributionUrl, availableFrom, availableUntil, syncedAt);
        this.catalogSource = catalogSource;
    }
}
