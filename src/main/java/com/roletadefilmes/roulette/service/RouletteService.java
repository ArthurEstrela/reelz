package com.roletadefilmes.roulette.service;

import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.observability.ReelzMetrics;
import com.roletadefilmes.observability.ReelzMetrics.RouletteSpinOutcome;
import com.roletadefilmes.roulette.api.dto.RouletteMovieResponse;
import com.roletadefilmes.roulette.api.dto.RouletteSpinRequest;
import com.roletadefilmes.roulette.api.dto.RouletteSpinResponse;
import com.roletadefilmes.roulette.api.dto.SpinQuotaResponse;
import com.roletadefilmes.roulette.api.dto.StreamingAvailabilityResponse;
import com.roletadefilmes.roulette.domain.RouletteSpinStatus;
import com.roletadefilmes.roulette.domain.exception.DailyLimitExceededException;
import com.roletadefilmes.roulette.domain.exception.DuplicateSpinException;
import com.roletadefilmes.roulette.domain.exception.EmptyProviderSelectionException;
import com.roletadefilmes.roulette.domain.exception.FreePlanProviderLimitException;
import com.roletadefilmes.roulette.domain.exception.NoMoviesFoundException;
import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import com.roletadefilmes.roulette.persistence.entity.RouletteSpinEntity;
import com.roletadefilmes.roulette.persistence.repository.RouletteDailyUsageRepository;
import com.roletadefilmes.roulette.persistence.repository.RouletteSpinRepository;
import com.roletadefilmes.streaming.domain.MonetizationType;
import com.roletadefilmes.streaming.persistence.entity.MovieStreamingOfferEntity;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RouletteService {

    public static final int FREE_DAILY_SPIN_LIMIT = RouletteQuotaPolicy.FREE_DAILY_SPIN_LIMIT;

    private static final Set<MonetizationType> ELIGIBLE_MONETIZATION_TYPES = Set.of(
            MonetizationType.FLATRATE,
            MonetizationType.FREE,
            MonetizationType.ADS
    );

    private final UserAccountRepository userRepository;
    private final RouletteDailyUsageRepository dailyUsageRepository;
    private final MovieCacheRepository movieRepository;
    private final RouletteSpinRepository spinRepository;
    private final MovieStreamingOfferRepository offerRepository;
    private final Clock clock;
    private final ReelzMetrics metrics;

    public RouletteService(
            UserAccountRepository userRepository,
            RouletteDailyUsageRepository dailyUsageRepository,
            MovieCacheRepository movieRepository,
            RouletteSpinRepository spinRepository,
            MovieStreamingOfferRepository offerRepository,
            Clock clock,
            ReelzMetrics metrics
    ) {
        this.userRepository = userRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.movieRepository = movieRepository;
        this.spinRepository = spinRepository;
        this.offerRepository = offerRepository;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public RouletteSpinResponse spin(UUID userId, RouletteSpinRequest request) {
        var sample = metrics.startRouletteSpin();
        var plan = "unknown";

        try {
            var providerIds = validateAndSortProviderIds(request.providerIds());
            var now = Instant.now(clock);
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            var premium = user.isPremiumAt(now);
            plan = premium ? "premium" : "free";

            validatePlanProviderLimit(premium, providerIds);

            var usageDate = currentDateFor(user);
            var usage = getOrCreateDailyUsage(userId, user, usageDate);
            var auditFilters = buildAuditFilters(request, providerIds, user.getCountryCode());

            var existingSpin = spinRepository.findByUserIdAndIdempotencyKey(
                    userId,
                    request.idempotencyKey().toString()
            );
            if (existingSpin.isPresent()) {
                var response = replaySuccessfulSpinOrFail(
                        existingSpin.orElseThrow(),
                        usage,
                        premium,
                        providerIds,
                        auditFilters
                );
                metrics.recordRouletteSpinAfterCommit(sample, RouletteSpinOutcome.REPLAYED, plan);
                return response;
            }

            ensureSpinIsAvailable(usage, premium);

            var movie = movieRepository.findRandomAvailableMovie(
                            userId,
                            providerIds,
                            user.getCountryCode(),
                            request.genreId(),
                            request.vibeId()
                    )
                    .orElseThrow(NoMoviesFoundException::new);

            consumeSpin(usage, premium);
            dailyUsageRepository.save(usage);

            var spin = new RouletteSpinEntity(
                    user,
                    request.idempotencyKey().toString(),
                    auditFilters
            );
            spin.succeedWith(movie, now);
            spinRepository.save(spin);

            var response = buildResponse(
                    movie,
                    usage,
                    premium,
                    providerIds,
                    user.getCountryCode(),
                    now
            );
            metrics.recordRouletteSpinAfterCommit(sample, RouletteSpinOutcome.SUCCESS, plan);
            return response;
        } catch (DailyLimitExceededException exception) {
            metrics.recordRouletteSpin(sample, RouletteSpinOutcome.LIMIT_EXCEEDED, plan);
            throw exception;
        } catch (NoMoviesFoundException exception) {
            metrics.recordRouletteSpin(sample, RouletteSpinOutcome.NO_MOVIES, plan);
            throw exception;
        } catch (EmptyProviderSelectionException
                 | FreePlanProviderLimitException
                 | DuplicateSpinException exception) {
            metrics.recordRouletteSpin(sample, RouletteSpinOutcome.INVALID_REQUEST, plan);
            throw exception;
        } catch (RuntimeException exception) {
            metrics.recordRouletteSpin(sample, RouletteSpinOutcome.ERROR, plan);
            throw exception;
        }
    }

    private List<UUID> validateAndSortProviderIds(Set<UUID> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            throw new EmptyProviderSelectionException();
        }
        return providerIds.stream().sorted().toList();
    }

    private void validatePlanProviderLimit(boolean premium, List<UUID> providerIds) {
        if (!premium && providerIds.size() > 1) {
            throw new FreePlanProviderLimitException();
        }
    }

    private LocalDate currentDateFor(UserAccountEntity user) {
        var userClock = clock.withZone(ZoneId.of(user.getTimezone()));
        return LocalDate.now(userClock);
    }

    private RouletteDailyUsageEntity getOrCreateDailyUsage(
            UUID userId,
            UserAccountEntity user,
            LocalDate usageDate
    ) {
        var existing = dailyUsageRepository.findByUserIdAndUsageDate(userId, usageDate);
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }

        dailyUsageRepository.createIfAbsent(
                UUID.randomUUID(),
                userId,
                usageDate,
                user.getTimezone()
        );
        return dailyUsageRepository.findByUserIdAndUsageDate(userId, usageDate)
                .orElseThrow(() -> new IllegalStateException("Could not initialize daily roulette usage"));
    }

    private void ensureSpinIsAvailable(RouletteDailyUsageEntity usage, boolean premium) {
        if (!premium
                && usage.getBaseSpinsUsed() >= FREE_DAILY_SPIN_LIMIT
                && usage.getRewardedSpinsRemaining() == 0) {
            throw new DailyLimitExceededException();
        }
    }

    private void consumeSpin(RouletteDailyUsageEntity usage, boolean premium) {
        if (premium) {
            return;
        }
        if (usage.getBaseSpinsUsed() < FREE_DAILY_SPIN_LIMIT) {
            usage.consumeBaseSpin();
            return;
        }
        usage.consumeRewardedSpin();
    }

    private RouletteSpinResponse replaySuccessfulSpinOrFail(
            RouletteSpinEntity spin,
            RouletteDailyUsageEntity usage,
            boolean premium,
            List<UUID> providerIds,
            Map<String, Object> requestedFilters
    ) {
        if (spin.getStatus() != RouletteSpinStatus.SUCCEEDED
                || spin.getMovie() == null
                || !spin.getFilters().equals(requestedFilters)) {
            throw new DuplicateSpinException();
        }
        return buildResponse(
                spin.getMovie(),
                usage,
                premium,
                providerIds,
                spin.getUser().getCountryCode(),
                Instant.now(clock)
        );
    }

    private Map<String, Object> buildAuditFilters(
            RouletteSpinRequest request,
            List<UUID> providerIds,
            String countryCode
    ) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("providerIds", providerIds.stream().map(UUID::toString).toList());
        filters.put("countryCode", countryCode);
        if (request.genreId() != null) {
            filters.put("genreId", request.genreId());
        }
        if (request.vibeId() != null) {
            filters.put("vibeId", request.vibeId().toString());
        }
        if (request.sessionId() != null) {
            filters.put("sessionId", request.sessionId().toString());
        }
        return filters;
    }

    private RouletteSpinResponse buildResponse(
            MovieCacheEntity movie,
            RouletteDailyUsageEntity usage,
            boolean premium,
            List<UUID> selectedProviderIds,
            String countryCode,
            Instant now
    ) {
        var availability = findEligibleAvailability(movie, selectedProviderIds, countryCode, now);
        var movieResponse = new RouletteMovieResponse(
                movie.getId(),
                movie.getTmdbId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getPosterPath(),
                movie.getReleaseDate(),
                movie.getVoteAverage(),
                availability
        );
        return new RouletteSpinResponse(movieResponse, buildQuotaResponse(usage, premium));
    }

    private List<StreamingAvailabilityResponse> findEligibleAvailability(
            MovieCacheEntity movie,
            List<UUID> selectedProviderIds,
            String countryCode,
            Instant now
    ) {
        var selectedProviders = Set.copyOf(selectedProviderIds);
        List<StreamingAvailabilityResponse> availability = new ArrayList<>();

        for (MovieStreamingOfferEntity offer
                : offerRepository.findAllByMovieIdAndCountryCode(movie.getId(), countryCode)) {
            if (!selectedProviders.contains(offer.getProvider().getId())
                    || !ELIGIBLE_MONETIZATION_TYPES.contains(offer.getMonetizationType())
                    || !isCurrentlyAvailable(offer, now)) {
                continue;
            }
            var provider = offer.getProvider();
            availability.add(new StreamingAvailabilityResponse(
                    provider.getId(),
                    provider.getTmdbProviderId(),
                    provider.getName(),
                    provider.getLogoPath(),
                    offer.getMonetizationType(),
                    offer.getAttributionUrl()
            ));
        }
        return List.copyOf(availability);
    }

    private boolean isCurrentlyAvailable(MovieStreamingOfferEntity offer, Instant now) {
        return (offer.getAvailableFrom() == null || !offer.getAvailableFrom().isAfter(now))
                && (offer.getAvailableUntil() == null || offer.getAvailableUntil().isAfter(now));
    }

    private SpinQuotaResponse buildQuotaResponse(RouletteDailyUsageEntity usage, boolean premium) {
        return RouletteQuotaPolicy.toResponse(usage, premium);
    }
}
