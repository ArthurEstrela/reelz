package com.roletadefilmes.roulette.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.roulette.api.dto.RouletteSpinRequest;
import com.roletadefilmes.roulette.domain.exception.DailyLimitExceededException;
import com.roletadefilmes.roulette.domain.exception.NoMoviesFoundException;
import com.roletadefilmes.roulette.service.RouletteService;
import com.roletadefilmes.shared.api.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouletteControllerTest {

    @Mock
    private RouletteService rouletteService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-07-29T15:00:00Z"), ZoneOffset.UTC);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RouletteController(rouletteService))
                .setControllerAdvice(new GlobalExceptionHandler(clock))
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldReturnNotFoundWhenNoMovieMatches() throws Exception {
        when(rouletteService.spin(any(), any())).thenThrow(new NoMoviesFoundException());

        performSpin()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_MOVIES_FOUND"))
                .andExpect(jsonPath("$.message").value(
                        "Nenhum filme foi encontrado. Altere os serviços, o gênero ou a vibe selecionada."
                ));
    }

    @Test
    void shouldReturnTooManyRequestsWhenDailyLimitIsExceeded() throws Exception {
        when(rouletteService.spin(any(), any())).thenThrow(new DailyLimitExceededException());

        performSpin()
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DAILY_SPIN_LIMIT_EXCEEDED"));
    }

    private org.springframework.test.web.servlet.ResultActions performSpin() throws Exception {
        var userId = UUID.randomUUID();
        var request = new RouletteSpinRequest(
                UUID.randomUUID(),
                Set.of(UUID.randomUUID()),
                null,
                null
        );
        return mockMvc.perform(post("/api/v1/roulette/spin")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }
}
