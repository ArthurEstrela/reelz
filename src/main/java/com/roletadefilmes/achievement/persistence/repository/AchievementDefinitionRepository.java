package com.roletadefilmes.achievement.persistence.repository;

import com.roletadefilmes.achievement.persistence.entity.AchievementDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementDefinitionRepository
        extends JpaRepository<AchievementDefinitionEntity, UUID> {

    List<AchievementDefinitionEntity> findAllByActiveTrueOrderByDisplayOrder();
}
