package com.roletadefilmes.history.persistence.repository;

import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserMovieHistoryRepository extends JpaRepository<UserMovieHistoryEntity, UUID> {

    Optional<UserMovieHistoryEntity> findByUserIdAndMovieId(UUID userId, UUID movieId);

    @Query(
            value = """
                    SELECT history
                      FROM UserMovieHistoryEntity history
                      JOIN FETCH history.movie
                     WHERE history.user.id = :userId
                       AND history.status = :status
                    """,
            countQuery = """
                    SELECT COUNT(history)
                      FROM UserMovieHistoryEntity history
                     WHERE history.user.id = :userId
                       AND history.status = :status
                    """
    )
    Page<UserMovieHistoryEntity> findPageByUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") UserMovieStatus status,
            Pageable pageable
    );

    boolean existsByUserIdAndMovieIdAndStatus(UUID userId, UUID movieId, UserMovieStatus status);
}
