package com.roletadefilmes.streaming.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "streaming_provider")
public class StreamingProviderEntity extends AuditableUuidEntity {

    @Column(name = "tmdb_provider_id", nullable = false, unique = true)
    private Integer tmdbProviderId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "logo_path", length = 255)
    private String logoPath;

    @Column(name = "display_priority", nullable = false)
    private int displayPriority;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected StreamingProviderEntity() {
    }

    public StreamingProviderEntity(Integer tmdbProviderId, String name) {
        this.tmdbProviderId = tmdbProviderId;
        this.name = name;
    }

    public Integer getTmdbProviderId() {
        return tmdbProviderId;
    }

    public String getName() {
        return name;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public int getDisplayPriority() {
        return displayPriority;
    }

    public boolean isActive() {
        return active;
    }

    public void refreshCatalogData(String name, String logoPath, int displayPriority) {
        this.name = name;
        this.logoPath = logoPath;
        this.displayPriority = displayPriority;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
