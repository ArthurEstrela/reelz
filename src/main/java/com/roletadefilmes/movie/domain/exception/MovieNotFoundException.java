package com.roletadefilmes.movie.domain.exception;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long tmdbMovieId) {
        super("Filme não encontrado no cache para o ID do TMDB: " + tmdbMovieId);
    }
}
