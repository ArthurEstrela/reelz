package com.roletadefilmes.onboarding.service;

import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import com.roletadefilmes.history.persistence.repository.UserMovieHistoryRepository;
import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.observability.CineGiroMetrics;
import com.roletadefilmes.onboarding.api.dto.CompleteOnboardingRequest;
import com.roletadefilmes.onboarding.api.dto.CompleteOnboardingResponse;
import com.roletadefilmes.onboarding.api.dto.OnboardingMovieResponse;
import com.roletadefilmes.onboarding.api.dto.OnboardingMoviesResponse;
import com.roletadefilmes.onboarding.domain.exception.InvalidOnboardingSelectionException;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

@Service
public class OnboardingService {

    private final UserAccountRepository userRepository;
    private final MovieCacheRepository movieRepository;
    private final UserMovieHistoryRepository historyRepository;
    private final Clock clock;
    private final CineGiroMetrics metrics;

    public OnboardingService(
            UserAccountRepository userRepository,
            MovieCacheRepository movieRepository,
            UserMovieHistoryRepository historyRepository,
            Clock clock,
            CineGiroMetrics metrics
    ) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public OnboardingMoviesResponse getMovies(UUID userId, int limit) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var movies = movieRepository.findPopularForOnboarding(
                        userId,
                        user.getCountryCode(),
                        limit
                )
                .stream()
                .map(movie -> new OnboardingMovieResponse(
                        movie.getTmdbId(),
                        movie.getTitle(),
                        movie.getPosterPath(),
                        movie.getVoteAverage()
                ))
                .toList();

        return new OnboardingMoviesResponse(movies, limit);
    }

    @Transactional
    public CompleteOnboardingResponse complete(UUID userId, CompleteOnboardingRequest request) {
        if (!request.presentedMovieIds().containsAll(request.watchedMovieIds())) {
            throw new InvalidOnboardingSelectionException(
                    "A lista de assistidos deve conter apenas filmes apresentados no onboarding."
            );
        }

        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var firstCompletion = user.getOnboardingCompletedAt() == null;
        var movies = movieRepository.findAllByTmdbIdIn(request.presentedMovieIds());
        if (movies.size() != request.presentedMovieIds().size()) {
            throw new InvalidOnboardingSelectionException(
                    "Um ou mais filmes apresentados não existem no catálogo."
            );
        }

        var movieByTmdbId = new HashMap<Long, MovieCacheEntity>();
        movies.forEach(movie -> movieByTmdbId.put(movie.getTmdbId(), movie));
        var watchedMovieInternalIds = request.watchedMovieIds().stream()
                .map(movieByTmdbId::get)
                .map(MovieCacheEntity::getId)
                .toList();
        var historyByMovieId = new HashMap<UUID, UserMovieHistoryEntity>();
        historyRepository.findAllByUserIdAndMovieIdIn(userId, watchedMovieInternalIds)
                .forEach(history -> historyByMovieId.put(history.getMovie().getId(), history));

        var now = Instant.now(clock);
        var historiesToSave = new ArrayList<UserMovieHistoryEntity>();
        var watchedMoviesAdded = 0;

        for (var tmdbId : new HashSet<>(request.watchedMovieIds())) {
            var movie = movieByTmdbId.get(tmdbId);
            var existing = historyByMovieId.get(movie.getId());

            if (existing != null && existing.getStatus() == UserMovieStatus.WATCHED) {
                continue;
            }

            var history = existing != null
                    ? existing
                    : new UserMovieHistoryEntity(
                            user,
                            movie,
                            UserMovieStatus.WATCHED,
                            now,
                            null
                    );
            history.markAsWatched(now, history.getUserRating());
            historiesToSave.add(history);
            watchedMoviesAdded++;
        }

        historyRepository.saveAll(historiesToSave);
        user.completeOnboarding(now);
        if (firstCompletion) {
            metrics.recordOnboardingCompletionAfterCommit(watchedMoviesAdded);
        }

        return new CompleteOnboardingResponse(true, watchedMoviesAdded);
    }
}
