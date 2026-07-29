package com.roletadefilmes.reward.persistence.repository;

import com.roletadefilmes.reward.persistence.entity.RewardGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RewardGrantRepository extends JpaRepository<RewardGrantEntity, UUID> {

    boolean existsByAdProviderAndExternalRewardId(String adProvider, String externalRewardId);
}
