package com.roletadefilmes.vibe.persistence.entity;

import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "vibe")
public class VibeEntity extends AuditableUuidEntity {

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "genre_ids", nullable = false, columnDefinition = "integer[]")
    private Integer[] genreIds = new Integer[0];

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_rules", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> queryRules = new HashMap<>();

    @Column(name = "rules_version", nullable = false)
    private int rulesVersion = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected VibeEntity() {
    }

    public VibeEntity(String slug, String label, Integer[] genreIds) {
        this.slug = slug;
        this.label = label;
        this.genreIds = genreIds == null ? new Integer[0] : genreIds.clone();
    }

    public String getSlug() {
        return slug;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Integer[] getGenreIds() {
        return genreIds.clone();
    }

    public Map<String, Object> getQueryRules() {
        return Map.copyOf(queryRules);
    }

    public int getRulesVersion() {
        return rulesVersion;
    }

    public boolean isActive() {
        return active;
    }

    public void updateDefinition(
            String label,
            String description,
            Integer[] genreIds,
            Map<String, Object> queryRules,
            int rulesVersion
    ) {
        this.label = label;
        this.description = description;
        this.genreIds = genreIds == null ? new Integer[0] : genreIds.clone();
        this.queryRules = queryRules == null ? new HashMap<>() : new HashMap<>(queryRules);
        this.rulesVersion = rulesVersion;
    }

    public void deactivate() {
        this.active = false;
    }
}
