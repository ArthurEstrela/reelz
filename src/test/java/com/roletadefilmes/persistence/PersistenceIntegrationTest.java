package com.roletadefilmes.persistence;

import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import com.roletadefilmes.history.persistence.repository.UserMovieHistoryRepository;
import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import com.roletadefilmes.roulette.persistence.repository.RouletteDailyUsageRepository;
import com.roletadefilmes.streaming.domain.MonetizationType;
import com.roletadefilmes.streaming.persistence.entity.MovieStreamingOfferEntity;
import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import com.roletadefilmes.streaming.persistence.entity.UserStreamingPreferenceEntity;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.streaming.persistence.repository.UserStreamingPreferenceRepository;
import com.roletadefilmes.support.PostgresRepositoryIntegrationTest;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import com.roletadefilmes.vibe.persistence.entity.VibeEntity;
import com.roletadefilmes.vibe.persistence.repository.VibeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceIntegrationTest extends PostgresRepositoryIntegrationTest {

    private final UserAccountRepository userRepository;
    private final MovieCacheRepository movieRepository;
    private final UserMovieHistoryRepository historyRepository;
    private final StreamingProviderRepository providerRepository;
    private final MovieStreamingOfferRepository offerRepository;
    private final UserStreamingPreferenceRepository preferenceRepository;
    private final VibeRepository vibeRepository;
    private final RouletteDailyUsageRepository dailyUsageRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate requiresNewTransaction;

    @Autowired
    PersistenceIntegrationTest(
            UserAccountRepository userRepository,
            MovieCacheRepository movieRepository,
            UserMovieHistoryRepository historyRepository,
            StreamingProviderRepository providerRepository,
            MovieStreamingOfferRepository offerRepository,
            UserStreamingPreferenceRepository preferenceRepository,
            VibeRepository vibeRepository,
            RouletteDailyUsageRepository dailyUsageRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.historyRepository = historyRepository;
        this.providerRepository = providerRepository;
        this.offerRepository = offerRepository;
        this.preferenceRepository = preferenceRepository;
        this.vibeRepository = vibeRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.entityManager = entityManager;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
    }

    @Test
    void shouldPersistAndReloadAValidHistoryEntry() {
        var user = userRepository.save(newUser("history@cinegiro.app"));
        var movie = movieRepository.save(newMovie(550L, "Clube da Luta", 18));

        var history = historyRepository.saveAndFlush(new UserMovieHistoryEntity(
                user,
                movie,
                UserMovieStatus.WATCHED,
                Instant.now().minus(1, ChronoUnit.DAYS),
                5
        ));
        entityManager.clear();

        var reloaded = historyRepository.findById(history.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(UserMovieStatus.WATCHED);
        assertThat(reloaded.getUserRating()).isEqualTo(5);
    }

    @Test
    void shouldRejectRatingOutsideOneToFive() {
        var user = userRepository.save(newUser("rating@cinegiro.app"));
        var movie = movieRepository.save(newMovie(551L, "Filme inválido", 18));

        var invalidHistory = new UserMovieHistoryEntity(
                user,
                movie,
                UserMovieStatus.WATCHED,
                Instant.now(),
                6
        );

        assertThatThrownBy(() -> historyRepository.saveAndFlush(invalidHistory))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_user_movie_history_rating");
    }

    @Test
    void shouldRejectFutureWatchedDate() {
        var user = userRepository.save(newUser("future@cinegiro.app"));
        var movie = movieRepository.save(newMovie(552L, "Filme do futuro", 878));

        var invalidHistory = new UserMovieHistoryEntity(
                user,
                movie,
                UserMovieStatus.WATCHED,
                Instant.now().plus(1, ChronoUnit.DAYS),
                4
        );

        assertThatThrownBy(() -> historyRepository.saveAndFlush(invalidHistory))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("watched_at cannot be in the future");
    }

    @Test
    void shouldRejectWatchedMetadataForWatchlistStatus() {
        var user = userRepository.save(newUser("watchlist@cinegiro.app"));
        var movie = movieRepository.save(newMovie(553L, "Watchlist inválida", 12));

        var invalidHistory = new UserMovieHistoryEntity(
                user,
                movie,
                UserMovieStatus.WATCHLIST,
                Instant.now(),
                3
        );

        assertThatThrownBy(() -> historyRepository.saveAndFlush(invalidHistory))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_user_movie_history_watchlist_metadata");
    }

    @Test
    void shouldFindOnlyAnUnwatchedMovieMatchingProviderGenreAndVibe() {
        var user = userRepository.save(newUser("roulette@cinegiro.app"));
        var netflix = providerRepository.save(new StreamingProviderEntity(8, "Netflix"));
        var max = providerRepository.save(new StreamingProviderEntity(1899, "Max"));
        preferenceRepository.save(new UserStreamingPreferenceEntity(user, netflix));

        var eligible = movieRepository.save(newMovie(1001L, "Comédia elegível", 35));
        var watched = movieRepository.save(newMovie(1002L, "Comédia já assistida", 35));
        var wrongProvider = movieRepository.save(newMovie(1003L, "Comédia no Max", 35));
        var wrongGenre = movieRepository.save(newMovie(1004L, "Drama na Netflix", 18));

        offerRepository.saveAll(List.of(
                newOffer(eligible, netflix),
                newOffer(watched, netflix),
                newOffer(wrongProvider, max),
                newOffer(wrongGenre, netflix)
        ));
        historyRepository.save(new UserMovieHistoryEntity(
                user,
                watched,
                UserMovieStatus.WATCHED,
                Instant.now(),
                null
        ));
        var funny = vibeRepository.save(new VibeEntity("test-para-rir", "Para rir", new Integer[]{35}));
        entityManager.flush();
        entityManager.clear();

        var result = movieRepository.findRandomAvailableMovie(
                user.getId(),
                List.of(netflix.getId()),
                "BR",
                "ALL",
                35,
                funny.getId()
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(eligible.getId());

        var resultWithoutOptionalFilters = movieRepository.findRandomAvailableMovie(
                user.getId(),
                List.of(netflix.getId()),
                "BR",
                "ALL",
                null,
                null
        );

        assertThat(resultWithoutOptionalFilters).isPresent();
        assertThat(resultWithoutOptionalFilters.orElseThrow().getId())
                .isIn(eligible.getId(), wrongGenre.getId());
    }

    @Test
    void shouldFilterRouletteCandidatesByCatalogSourceWithoutDeletingData() {
        var user = userRepository.save(newUser("catalog-source@cinegiro.app"));
        var netflix = providerRepository.save(new StreamingProviderEntity(8, "Netflix"));
        var tmdbMovie = movieRepository.save(newMovie(2001L, "Filme TMDB", 18));
        var streamingAvailabilityMovie = movieRepository.save(
                newMovie(2002L, "Filme Movie of the Night", 18)
        );
        var tmdbOffer = newOffer(tmdbMovie, netflix);
        var streamingAvailabilityOffer = newOffer(streamingAvailabilityMovie, netflix);
        streamingAvailabilityOffer.refreshAvailability(
                "https://streaming.example/watch/2002",
                null,
                null,
                Instant.now(),
                "STREAMING_AVAILABILITY"
        );
        offerRepository.saveAll(List.of(tmdbOffer, streamingAvailabilityOffer));
        entityManager.flush();
        entityManager.clear();

        var tmdbResult = movieRepository.findRandomAvailableMovie(
                user.getId(),
                List.of(netflix.getId()),
                "BR",
                "TMDB",
                null,
                null
        );
        var streamingAvailabilityResult = movieRepository.findRandomAvailableMovie(
                user.getId(),
                List.of(netflix.getId()),
                "BR",
                "STREAMING_AVAILABILITY",
                null,
                null
        );
        var allSourcesResult = movieRepository.findRandomAvailableMovie(
                user.getId(),
                List.of(netflix.getId()),
                "BR",
                "ALL",
                null,
                null
        );

        assertThat(tmdbResult).get().extracting(MovieCacheEntity::getId).isEqualTo(tmdbMovie.getId());
        assertThat(streamingAvailabilityResult).get()
                .extracting(MovieCacheEntity::getId)
                .isEqualTo(streamingAvailabilityMovie.getId());
        assertThat(allSourcesResult).get()
                .extracting(MovieCacheEntity::getId)
                .isIn(tmdbMovie.getId(), streamingAvailabilityMovie.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRejectAStaleDailyUsageUpdateWithOptimisticLocking() {
        UUID usageId = requiresNewTransaction.execute(status -> {
            var user = userRepository.save(newUser("locking@cinegiro.app"));
            var usage = dailyUsageRepository.saveAndFlush(new RouletteDailyUsageEntity(
                    user,
                    LocalDate.now(),
                    "America/Sao_Paulo"
            ));
            return usage.getId();
        });

        var firstCopy = requiresNewTransaction.execute(status ->
                dailyUsageRepository.findById(usageId).orElseThrow());
        var staleCopy = requiresNewTransaction.execute(status ->
                dailyUsageRepository.findById(usageId).orElseThrow());

        firstCopy.consumeBaseSpin();
        requiresNewTransaction.executeWithoutResult(status -> dailyUsageRepository.saveAndFlush(firstCopy));

        staleCopy.consumeBaseSpin();
        assertThatThrownBy(() -> requiresNewTransaction.executeWithoutResult(
                status -> dailyUsageRepository.saveAndFlush(staleCopy)
        )).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void shouldCreateOnlyOneDailyUsageForTheSameUserAndDate() {
        var user = userRepository.saveAndFlush(newUser("daily-usage@cinegiro.app"));
        var usageDate = LocalDate.of(2026, 7, 29);

        var firstInsert = dailyUsageRepository.createIfAbsent(
                UUID.randomUUID(),
                user.getId(),
                usageDate,
                user.getTimezone()
        );
        var duplicateInsert = dailyUsageRepository.createIfAbsent(
                UUID.randomUUID(),
                user.getId(),
                usageDate,
                user.getTimezone()
        );

        assertThat(firstInsert).isOne();
        assertThat(duplicateInsert).isZero();
        assertThat(dailyUsageRepository.findByUserIdAndUsageDate(user.getId(), usageDate))
                .isPresent();
    }

    private UserAccountEntity newUser(String email) {
        return new UserAccountEntity(
                email,
                "hash-de-senha-para-teste",
                "Pessoa de Teste",
                "America/Sao_Paulo",
                "BR"
        );
    }

    private MovieCacheEntity newMovie(Long tmdbId, String title, Integer genreId) {
        return new MovieCacheEntity(tmdbId, title, new Integer[]{genreId}, Instant.now());
    }

    private MovieStreamingOfferEntity newOffer(
            MovieCacheEntity movie,
            StreamingProviderEntity provider
    ) {
        return new MovieStreamingOfferEntity(
                movie,
                provider,
                "BR",
                MonetizationType.FLATRATE,
                Instant.now()
        );
    }
}
