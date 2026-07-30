package com.roletadefilmes.history.service;

import com.roletadefilmes.history.api.dto.HistoryResponse;
import com.roletadefilmes.history.api.dto.SaveHistoryRequest;
import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import com.roletadefilmes.history.persistence.repository.UserMovieHistoryRepository;
import com.roletadefilmes.movie.domain.exception.MovieNotFoundException;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class HistoryService {

    private final UserAccountRepository userRepository;
    private final MovieCacheRepository movieRepository;
    private final UserMovieHistoryRepository historyRepository;
    private final Clock clock;

    public HistoryService(
            UserAccountRepository userRepository,
            MovieCacheRepository movieRepository,
            UserMovieHistoryRepository historyRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    @Transactional
    public HistoryResponse save(UUID userId, SaveHistoryRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var movie = movieRepository.findByTmdbId(request.movieId())
                .orElseThrow(() -> new MovieNotFoundException(request.movieId()));
        var now = Instant.now(clock);

        var history = historyRepository.findByUserIdAndMovieId(userId, movie.getId())
                .orElseGet(() -> new UserMovieHistoryEntity(
                        user,
                        movie,
                        request.status(),
                        watchedAtFor(request.status(), now),
                        null
                ));

        if (request.status() == UserMovieStatus.WATCHED) {
            history.markAsWatched(now, history.getUserRating());
        } else {
            history.moveToWatchlist();
        }

        var saved = historyRepository.saveAndFlush(history);
        return new HistoryResponse(
                saved.getId(),
                saved.getMovie().getTmdbId(),
                saved.getStatus(),
                saved.getWatchedAt(),
                saved.getUserRating(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private Instant watchedAtFor(UserMovieStatus status, Instant now) {
        return status == UserMovieStatus.WATCHED ? now : null;
    }
}
