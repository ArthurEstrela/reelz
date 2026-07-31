package com.roletadefilmes.catalog.integration.tmdb;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TmdbMovieData(
        long tmdbId,
        String title,
        String originalTitle,
        String overview,
        String posterPath,
        LocalDate releaseDate,
        BigDecimal voteAverage,
        int voteCount,
        List<Integer> genreIds,
        boolean adult,
        String originalLanguage
) {
}
