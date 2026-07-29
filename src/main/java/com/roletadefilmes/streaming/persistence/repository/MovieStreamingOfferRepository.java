package com.roletadefilmes.streaming.persistence.repository;

import com.roletadefilmes.streaming.persistence.entity.MovieStreamingOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovieStreamingOfferRepository extends JpaRepository<MovieStreamingOfferEntity, UUID> {

    List<MovieStreamingOfferEntity> findAllByMovieIdAndCountryCode(UUID movieId, String countryCode);
}
