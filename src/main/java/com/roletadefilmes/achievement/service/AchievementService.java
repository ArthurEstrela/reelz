package com.roletadefilmes.achievement.service;

import com.roletadefilmes.achievement.api.dto.AchievementOverviewResponse;
import com.roletadefilmes.achievement.api.dto.AchievementResponse;
import com.roletadefilmes.achievement.domain.AchievementCode;
import com.roletadefilmes.achievement.persistence.entity.UserAchievementEntity;
import com.roletadefilmes.achievement.persistence.repository.AchievementDefinitionRepository;
import com.roletadefilmes.achievement.persistence.repository.UserAchievementRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class AchievementService {

    private final AchievementDefinitionRepository definitionRepository;
    private final UserAchievementRepository progressRepository;
    private final UserAccountRepository userRepository;
    private final Clock clock;

    public AchievementService(
            AchievementDefinitionRepository definitionRepository,
            UserAchievementRepository progressRepository,
            UserAccountRepository userRepository,
            Clock clock
    ) {
        this.definitionRepository = definitionRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public AchievementOverviewResponse getOverview(UUID userId) {
        progressRepository.lockUserProgress(userId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var definitions = definitionRepository.findAllByActiveTrueOrderByDisplayOrder();
        var existingByCode = progressRepository.findAllWithDefinitionByUserId(userId).stream()
                .collect(Collectors.toMap(
                        progress -> progress.getAchievement().getCode(),
                        progress -> progress
                ));
        var measuredProgress = measureProgress(userId);
        var evaluatedAt = Instant.now(clock);

        var progressRows = definitions.stream().map(definition -> {
            var progress = existingByCode.get(definition.getCode());
            if (progress == null) {
                var measured = measuredProgress.getOrDefault(definition.getCode(), 0L);
                progress = new UserAchievementEntity(
                        user,
                        definition,
                        measured,
                        measured >= definition.getTargetValue() ? evaluatedAt : null
                );
            } else {
                progress.refresh(
                        measuredProgress.getOrDefault(definition.getCode(), 0L),
                        evaluatedAt
                );
            }
            return progress;
        }).toList();

        var saved = progressRepository.saveAllAndFlush(progressRows);
        var savedByCode = saved.stream().collect(Collectors.toMap(
                progress -> progress.getAchievement().getCode(),
                progress -> progress
        ));
        var items = definitions.stream().map(definition -> {
            var progress = savedByCode.get(definition.getCode());
            return new AchievementResponse(
                    definition.getCode(),
                    definition.getName(),
                    definition.getDescription(),
                    definition.getIconKey(),
                    definition.getCategory(),
                    definition.getTargetValue(),
                    progress.getProgressValue(),
                    progress.getUnlockedAt() != null,
                    progress.getUnlockedAt()
            );
        }).toList();
        var unlockedCount = Math.toIntExact(items.stream().filter(AchievementResponse::unlocked).count());
        return new AchievementOverviewResponse(unlockedCount, items.size(), items);
    }

    private EnumMap<AchievementCode, Long> measureProgress(UUID userId) {
        var result = new EnumMap<AchievementCode, Long>(AchievementCode.class);
        var watched = lazy(() -> progressRepository.countHistory(userId, "WATCHED"));
        var successfulSpins = lazy(() -> progressRepository.countSuccessfulSpins(userId));

        result.put(AchievementCode.FIRST_SPIN, successfulSpins.getAsLong());
        result.put(AchievementCode.OPEN_PROVIDER, progressRepository.countProviderOpens(userId));
        result.put(AchievementCode.WATCHED_10, watched.getAsLong());
        result.put(AchievementCode.WATCHED_50, watched.getAsLong());
        result.put(AchievementCode.WATCHED_100, watched.getAsLong());
        result.put(AchievementCode.WATCHLIST_5, progressRepository.countHistory(userId, "WATCHLIST"));
        result.put(AchievementCode.GENRES_5, progressRepository.countWatchedGenres(userId));
        result.put(AchievementCode.COUPLE_SPIN, progressRepository.countCoupleSpins(userId));
        result.put(
                AchievementCode.GROUP_SPIN_3,
                progressRepository.countGroupSpinsWithThreeMembers(userId)
        );
        result.put(AchievementCode.ACTIVE_WEEKS_4, progressRepository.countActiveWeeks(userId));
        return result;
    }

    private LongSupplier lazy(LongSupplier delegate) {
        return new LongSupplier() {
            private Long value;

            @Override
            public long getAsLong() {
                if (value == null) value = delegate.getAsLong();
                return value;
            }
        };
    }
}
