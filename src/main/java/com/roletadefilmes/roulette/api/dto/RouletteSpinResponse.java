package com.roletadefilmes.roulette.api.dto;

public record RouletteSpinResponse(
        RouletteMovieResponse movie,
        SpinQuotaResponse quota
) {
}
