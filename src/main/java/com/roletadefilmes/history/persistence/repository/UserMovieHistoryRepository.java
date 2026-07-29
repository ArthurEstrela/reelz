package com.roletadefilmes.history.persistence.repository;

import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMovieHistoryRepository extends JpaRepository<UserMovieHistoryEntity, UUID> {

    Optional<UserMovieHistoryEntity> findByUserIdAndMovieId(UUID userId, UUID movieId);

    List<UserMovieHistoryEntity> findAllByUserIdAndStatusOrderByUpdatedAtDesc(
            UUID userId,
            UserMovieStatus status
    );

    boolean existsByUserIdAndMovieIdAndStatus(UUID userId, UUID movieId, UserMovieStatus status);
}
