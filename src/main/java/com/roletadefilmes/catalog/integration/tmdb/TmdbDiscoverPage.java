package com.roletadefilmes.catalog.integration.tmdb;

import java.util.List;

public record TmdbDiscoverPage(
        List<TmdbMovieData> movies,
        int totalPages
) {
}
