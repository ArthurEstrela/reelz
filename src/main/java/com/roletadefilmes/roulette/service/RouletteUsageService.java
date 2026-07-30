package com.roletadefilmes.roulette.service;

import com.roletadefilmes.roulette.api.dto.SpinQuotaResponse;
import com.roletadefilmes.roulette.persistence.repository.RouletteDailyUsageRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class RouletteUsageService {

    private final UserAccountRepository userRepository;
    private final RouletteDailyUsageRepository dailyUsageRepository;
    private final Clock clock;

    public RouletteUsageService(
            UserAccountRepository userRepository,
            RouletteDailyUsageRepository dailyUsageRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SpinQuotaResponse getToday(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var now = Instant.now(clock);
        var usageDate = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
        var usage = dailyUsageRepository.findByUserIdAndUsageDate(userId, usageDate)
                .orElse(null);

        return RouletteQuotaPolicy.toResponse(usage, user.isPremiumAt(now));
    }
}
