package com.roletadefilmes.roulette.service;

import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.roulette.api.dto.RouletteSpinRequest;
import com.roletadefilmes.roulette.domain.RouletteSpinStatus;
import com.roletadefilmes.roulette.domain.exception.DailyLimitExceededException;
import com.roletadefilmes.roulette.domain.exception.DuplicateSpinException;
import com.roletadefilmes.roulette.domain.exception.NoMoviesFoundException;
import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import com.roletadefilmes.roulette.persistence.entity.RouletteSpinEntity;
import com.roletadefilmes.roulette.persistence.repository.RouletteDailyUsageRepository;
import com.roletadefilmes.roulette.persistence.repository.RouletteSpinRepository;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouletteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-29T15:00:00Z");
    private static final LocalDate USAGE_DATE = LocalDate.of(2026, 7, 29);

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private RouletteDailyUsageRepository dailyUsageRepository;

    @Mock
    private MovieCacheRepository movieRepository;

    @Mock
    private RouletteSpinRepository spinRepository;

    @Mock
    private MovieStreamingOfferRepository offerRepository;

    private RouletteService service;

    @BeforeEach
    void setUp() {
        service = new RouletteService(
                userRepository,
                dailyUsageRepository,
                movieRepository,
                spinRepository,
                offerRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldRespectTheFreeDailyLimit() {
        var userId = UUID.randomUUID();
        var providerId = UUID.randomUUID();
        var user = newFreeUser();
        var usage = new RouletteDailyUsageEntity(user, USAGE_DATE, user.getTimezone());
        for (int spin = 0; spin < RouletteService.FREE_DAILY_SPIN_LIMIT; spin++) {
            usage.consumeBaseSpin();
        }
        arrangeUserAndUsage(userId, user, usage);
        var request = request(providerId);

        assertThatThrownBy(() -> service.spin(userId, request))
                .isInstanceOf(DailyLimitExceededException.class);

        verifyNoInteractions(movieRepository);
        verify(dailyUsageRepository, never()).save(any());
        verify(spinRepository, never()).save(any());
    }

    @Test
    void shouldNotConsumeASpinWhenNoMovieMatches() {
        var userId = UUID.randomUUID();
        var providerId = UUID.randomUUID();
        var user = newFreeUser();
        var usage = new RouletteDailyUsageEntity(user, USAGE_DATE, user.getTimezone());
        arrangeUserAndUsage(userId, user, usage);
        when(movieRepository.findRandomAvailableMovie(
                userId,
                List.of(providerId),
                "BR",
                null,
                null
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.spin(userId, request(providerId)))
                .isInstanceOf(NoMoviesFoundException.class);

        assertThat(usage.getBaseSpinsUsed()).isZero();
        verify(dailyUsageRepository, never()).save(any());
        verify(spinRepository, never()).save(any());
    }

    @Test
    void shouldConsumeOneSpinAndAuditASuccessfulResult() {
        var userId = UUID.randomUUID();
        var providerId = UUID.randomUUID();
        var user = newFreeUser();
        var usage = new RouletteDailyUsageEntity(user, USAGE_DATE, user.getTimezone());
        var movie = new MovieCacheEntity(550L, "Clube da Luta", new Integer[]{18}, NOW);
        arrangeUserAndUsage(userId, user, usage);
        when(movieRepository.findRandomAvailableMovie(
                userId,
                List.of(providerId),
                "BR",
                null,
                null
        )).thenReturn(Optional.of(movie));
        when(offerRepository.findAllByMovieIdAndCountryCode(movie.getId(), "BR"))
                .thenReturn(List.of());

        var response = service.spin(userId, request(providerId));

        assertThat(usage.getBaseSpinsUsed()).isEqualTo(1);
        assertThat(response.movie().tmdbId()).isEqualTo(550L);
        assertThat(response.quota().remainingDailySpins()).isEqualTo(4);
        verify(dailyUsageRepository).save(usage);

        var spinCaptor = ArgumentCaptor.forClass(RouletteSpinEntity.class);
        verify(spinRepository).save(spinCaptor.capture());
        assertThat(spinCaptor.getValue().getStatus()).isEqualTo(RouletteSpinStatus.SUCCEEDED);
        assertThat(spinCaptor.getValue().getMovie()).isSameAs(movie);
    }

    @Test
    void shouldNotConsumeTheFreeQuotaForAPremiumUser() {
        var userId = UUID.randomUUID();
        var providerId = UUID.randomUUID();
        var user = newFreeUser();
        user.activatePremium(NOW.plusSeconds(3_600));
        var usage = new RouletteDailyUsageEntity(user, USAGE_DATE, user.getTimezone());
        var movie = new MovieCacheEntity(551L, "Filme Premium", new Integer[]{18}, NOW);
        arrangeUserAndUsage(userId, user, usage);
        when(movieRepository.findRandomAvailableMovie(
                userId,
                List.of(providerId),
                "BR",
                null,
                null
        )).thenReturn(Optional.of(movie));
        when(offerRepository.findAllByMovieIdAndCountryCode(movie.getId(), "BR"))
                .thenReturn(List.of());

        var response = service.spin(userId, request(providerId));

        assertThat(usage.getBaseSpinsUsed()).isZero();
        assertThat(response.quota().unlimited()).isTrue();
        assertThat(response.quota().remainingDailySpins()).isNull();
        verify(spinRepository).save(any(RouletteSpinEntity.class));
    }

    @Test
    void shouldRejectAnIdempotencyKeyReusedWithDifferentFilters() {
        var userId = UUID.randomUUID();
        var originalProviderId = UUID.randomUUID();
        var differentProviderId = UUID.randomUUID();
        var idempotencyKey = UUID.randomUUID();
        var user = newFreeUser();
        var usage = new RouletteDailyUsageEntity(user, USAGE_DATE, user.getTimezone());
        var movie = new MovieCacheEntity(552L, "Filme anterior", new Integer[]{18}, NOW);
        var existingSpin = new RouletteSpinEntity(
                user,
                idempotencyKey.toString(),
                Map.of(
                        "providerIds", List.of(originalProviderId.toString()),
                        "countryCode", "BR"
                )
        );
        existingSpin.succeedWith(movie, NOW);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(dailyUsageRepository.findByUserIdAndUsageDate(userId, USAGE_DATE))
                .thenReturn(Optional.of(usage));
        when(spinRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey.toString()))
                .thenReturn(Optional.of(existingSpin));
        var request = new RouletteSpinRequest(
                idempotencyKey,
                Set.of(differentProviderId),
                null,
                null
        );

        assertThatThrownBy(() -> service.spin(userId, request))
                .isInstanceOf(DuplicateSpinException.class);

        assertThat(usage.getBaseSpinsUsed()).isZero();
        verifyNoInteractions(movieRepository);
        verify(spinRepository, never()).save(any());
    }

    private void arrangeUserAndUsage(
            UUID userId,
            UserAccountEntity user,
            RouletteDailyUsageEntity usage
    ) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(dailyUsageRepository.findByUserIdAndUsageDate(userId, USAGE_DATE))
                .thenReturn(Optional.of(usage));
        when(spinRepository.findByUserIdAndIdempotencyKey(any(), any()))
                .thenReturn(Optional.empty());
    }

    private UserAccountEntity newFreeUser() {
        return new UserAccountEntity(
                "person@reelz.app",
                "password-hash",
                "Pessoa",
                "America/Sao_Paulo",
                "BR"
        );
    }

    private RouletteSpinRequest request(UUID providerId) {
        return new RouletteSpinRequest(UUID.randomUUID(), Set.of(providerId), null, null);
    }
}
