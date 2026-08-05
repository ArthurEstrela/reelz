package com.roletadefilmes.social.api.dto;

import com.roletadefilmes.roulette.api.dto.RouletteMovieResponse;
import com.roletadefilmes.roulette.api.dto.SpinQuotaResponse;

public record SocialSpinResponse(
        SocialRoomResponse room,
        RouletteMovieResponse movie,
        SpinQuotaResponse quota
) {
}
