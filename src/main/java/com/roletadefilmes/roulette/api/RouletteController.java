package com.roletadefilmes.roulette.api;

import com.roletadefilmes.roulette.api.dto.RouletteSpinRequest;
import com.roletadefilmes.roulette.api.dto.RouletteSpinResponse;
import com.roletadefilmes.roulette.service.RouletteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roulette")
public class RouletteController {

    private final RouletteService rouletteService;

    public RouletteController(RouletteService rouletteService) {
        this.rouletteService = rouletteService;
    }

    @PostMapping("/spin")
    public ResponseEntity<RouletteSpinResponse> spin(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RouletteSpinRequest request
    ) {
        return ResponseEntity.ok(rouletteService.spin(userId, request));
    }
}
