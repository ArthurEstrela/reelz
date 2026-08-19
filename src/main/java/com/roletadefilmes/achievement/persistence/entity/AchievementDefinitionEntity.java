package com.roletadefilmes.achievement.persistence.entity;

import com.roletadefilmes.achievement.domain.AchievementCategory;
import com.roletadefilmes.achievement.domain.AchievementCode;
import com.roletadefilmes.shared.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "achievement_definition")
public class AchievementDefinitionEntity extends AuditableUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 60)
    private AchievementCode code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 220)
    private String description;

    @Column(name = "icon_key", nullable = false, length = 40)
    private String iconKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AchievementCategory category;

    @Column(name = "target_value", nullable = false)
    private long targetValue;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    protected AchievementDefinitionEntity() {
    }

    public AchievementCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconKey() {
        return iconKey;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public long getTargetValue() {
        return targetValue;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}
