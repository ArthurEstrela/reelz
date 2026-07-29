package com.roletadefilmes.security;

import com.roletadefilmes.auth.api.AuthController;
import com.roletadefilmes.auth.api.dto.LoginResponse;
import com.roletadefilmes.auth.service.AuthService;
import com.roletadefilmes.roulette.api.RouletteController;
import com.roletadefilmes.roulette.service.RouletteService;
import com.roletadefilmes.shared.config.TimeConfiguration;
import com.roletadefilmes.support.security.WithMockReelzUser;
import com.roletadefilmes.user.api.UserController;
import com.roletadefilmes.user.service.UserRegistrationService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        RouletteController.class,
        AuthController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        TimeConfiguration.class
})
class SecurityIntegrationTest {

    private static final String MOCK_USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SPIN_BODY = """
            {
              "idempotencyKey": "eb00bf86-bb7f-4652-82b3-16a076ced021",
              "providerIds": ["c908fc1b-8038-4c78-ab08-b578ce0c92d2"],
              "genreId": null,
              "vibeId": null
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouletteService rouletteService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRegistrationService registrationService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldRejectAProtectedRouteWithoutAToken() throws Exception {
        mockMvc.perform(post("/api/v1/roulette/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SPIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldAuthenticateAValidBearerTokenAndUseItsSubject() throws Exception {
        var userId = UUID.randomUUID();
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);

        mockMvc.perform(post("/api/v1/roulette/spin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SPIN_BODY))
                .andExpect(status().isOk());

        verify(rouletteService).spin(eq(userId), any());
    }

    @Test
    @WithMockReelzUser(userId = MOCK_USER_ID)
    void shouldSupportTheTypedMockPrincipalInControllerTests() throws Exception {
        mockMvc.perform(post("/api/v1/roulette/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SPIN_BODY))
                .andExpect(status().isOk());

        verify(rouletteService).spin(eq(UUID.fromString(MOCK_USER_ID)), any());
    }

    @Test
    void shouldRejectAnInvalidBearerToken() throws Exception {
        when(jwtService.extractUserId("invalid-token"))
                .thenThrow(new MalformedJwtException("invalid token"));

        mockMvc.perform(post("/api/v1/roulette/spin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SPIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldAllowLoginWithoutAuthentication() throws Exception {
        when(authService.login(any())).thenReturn(
                new LoginResponse("token", "Bearer", 7_200, UUID.randomUUID())
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"person@reelz.app","password":"correct-password"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowRegistrationWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"Pessoa",
                                  "email":"person@reelz.app",
                                  "password":"correct-password",
                                  "timezone":"America/Sao_Paulo",
                                  "countryCode":"BR",
                                  "termsAccepted":true
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
