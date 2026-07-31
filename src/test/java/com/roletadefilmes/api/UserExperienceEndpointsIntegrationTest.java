package com.roletadefilmes.api;

import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import com.roletadefilmes.history.persistence.repository.UserMovieHistoryRepository;
import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import com.roletadefilmes.roulette.persistence.repository.RouletteDailyUsageRepository;
import com.roletadefilmes.security.JwtService;
import com.roletadefilmes.streaming.domain.MonetizationType;
import com.roletadefilmes.streaming.persistence.entity.MovieStreamingOfferEntity;
import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import com.roletadefilmes.vibe.persistence.entity.VibeEntity;
import com.roletadefilmes.vibe.persistence.repository.VibeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.jpa.open-in-view=false",
        "reelz.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "reelz.security.jwt.issuer=reelz-integration-test",
        "reelz.security.jwt.expiration=PT2H"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class UserExperienceEndpointsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("reelz_api_test")
                    .withUsername("reelz")
                    .withPassword("reelz");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private MovieCacheRepository movieRepository;

    @Autowired
    private UserMovieHistoryRepository historyRepository;

    @Autowired
    private RouletteDailyUsageRepository dailyUsageRepository;

    @Autowired
    private StreamingProviderRepository providerRepository;

    @Autowired
    private MovieStreamingOfferRepository offerRepository;

    @Autowired
    private VibeRepository vibeRepository;

    @Test
    void shouldCreateAndThenUpdateTheHistoryUsingTheTmdbMovieId() throws Exception {
        var user = userRepository.saveAndFlush(newUser("history-api@reelz.app"));
        movieRepository.saveAndFlush(new MovieCacheEntity(
                550L,
                "Clube da Luta",
                new Integer[]{18},
                Instant.now()
        ));

        mockMvc.perform(post("/api/v1/history")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550,"status":"WATCHLIST"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(550))
                .andExpect(jsonPath("$.status").value("WATCHLIST"))
                .andExpect(jsonPath("$.watchedAt").value(nullValue()))
                .andExpect(jsonPath("$.rating").value(nullValue()));

        mockMvc.perform(post("/api/v1/history")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550,"status":"WATCHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(550))
                .andExpect(jsonPath("$.status").value("WATCHED"))
                .andExpect(jsonPath("$.watchedAt").isNotEmpty())
                .andExpect(jsonPath("$.rating").value(nullValue()));

        assertThat(historyRepository.findAll()).hasSize(1);
        assertThat(historyRepository.findAll().getFirst().getWatchedAt()).isNotNull();
    }

    @Test
    void shouldReturnOnlyWatchedMoviesPaginatedByMostRecentWatchedDate() throws Exception {
        var user = userRepository.saveAndFlush(newUser("library-api@reelz.app"));
        var olderMovie = movieRepository.saveAndFlush(newMovie(
                100L,
                "Filme antigo",
                "/older.jpg",
                new BigDecimal("7.2")
        ));
        var newerMovie = movieRepository.saveAndFlush(newMovie(
                200L,
                "Filme recente",
                "/newer.jpg",
                new BigDecimal("8.6")
        ));
        var watchlistMovie = movieRepository.saveAndFlush(newMovie(
                300L,
                "Ainda quero ver",
                "/watchlist.jpg",
                new BigDecimal("6.5")
        ));
        var now = Instant.now();
        historyRepository.saveAllAndFlush(List.of(
                new UserMovieHistoryEntity(
                        user,
                        olderMovie,
                        UserMovieStatus.WATCHED,
                        now.minus(2, ChronoUnit.DAYS),
                        null
                ),
                new UserMovieHistoryEntity(
                        user,
                        newerMovie,
                        UserMovieStatus.WATCHED,
                        now.minus(1, ChronoUnit.DAYS),
                        5
                ),
                new UserMovieHistoryEntity(
                        user,
                        watchlistMovie,
                        UserMovieStatus.WATCHLIST,
                        null,
                        null
                )
        ));

        mockMvc.perform(get("/api/v1/history?page=0&size=1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].movieId").value(200))
                .andExpect(jsonPath("$.content[0].title").value("Filme recente"))
                .andExpect(jsonPath("$.content[0].posterPath").value("/newer.jpg"))
                .andExpect(jsonPath("$.content[0].tmdbRating").value(8.6))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.number").value(0));

        mockMvc.perform(get("/api/v1/history?page=1&size=1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movieId").value(100))
                .andExpect(jsonPath("$.content[0].title").value("Filme antigo"))
                .andExpect(jsonPath("$.page.number").value(1));
    }

    @Test
    void shouldReturnTheCurrentFreeQuotaIncludingRewardedSpins() throws Exception {
        var user = userRepository.saveAndFlush(newUser("usage-api@reelz.app"));
        var usage = new RouletteDailyUsageEntity(
                user,
                LocalDate.now(ZoneId.of(user.getTimezone())),
                user.getTimezone()
        );
        usage.consumeBaseSpin();
        usage.consumeBaseSpin();
        usage.grantRewardedSpins(3);
        usage.consumeRewardedSpin();
        dailyUsageRepository.saveAndFlush(usage);

        mockMvc.perform(get("/api/v1/roulette/usage/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlimited").value(false))
                .andExpect(jsonPath("$.dailyLimit").value(5))
                .andExpect(jsonPath("$.remainingDailySpins").value(3))
                .andExpect(jsonPath("$.remainingRewardedSpins").value(2));
    }

    @Test
    void shouldReturnTheFullFreeQuotaWhenThereIsNoUsageForToday() throws Exception {
        var user = userRepository.saveAndFlush(newUser("fresh-usage-api@reelz.app"));

        mockMvc.perform(get("/api/v1/roulette/usage/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlimited").value(false))
                .andExpect(jsonPath("$.dailyLimit").value(5))
                .andExpect(jsonPath("$.remainingDailySpins").value(5))
                .andExpect(jsonPath("$.remainingRewardedSpins").value(0));

        assertThat(dailyUsageRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnUnlimitedQuotaForPremiumWithoutCreatingDailyUsage() throws Exception {
        var user = newUser("premium-usage-api@reelz.app");
        user.activatePremium(Instant.now().plusSeconds(3_600));
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/v1/roulette/usage/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlimited").value(true))
                .andExpect(jsonPath("$.dailyLimit").value(nullValue()))
                .andExpect(jsonPath("$.remainingDailySpins").value(nullValue()))
                .andExpect(jsonPath("$.remainingRewardedSpins").value(0));

        assertThat(dailyUsageRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnOnlyActiveProviderAndVibeCatalogEntries() throws Exception {
        var user = userRepository.saveAndFlush(newUser("catalog-api@reelz.app"));
        var netflix = providerRepository.saveAndFlush(new StreamingProviderEntity(8, "Netflix"));
        var inactiveProvider = new StreamingProviderEntity(9, "Provider inativo");
        inactiveProvider.deactivate();
        providerRepository.saveAndFlush(inactiveProvider);
        var catalogMovie = movieRepository.saveAndFlush(newMovie(
                9001L,
                "Filme do catálogo",
                "/catalog.jpg",
                new BigDecimal("7.0")
        ));
        offerRepository.saveAndFlush(new MovieStreamingOfferEntity(
                catalogMovie,
                netflix,
                "BR",
                MonetizationType.FLATRATE,
                Instant.now()
        ));

        var funny = vibeRepository.saveAndFlush(
                new VibeEntity("catalog-test-para-rir", "Para rir", new Integer[]{35})
        );
        var inactiveVibe = new VibeEntity("arquivada", "Arquivada", new Integer[]{18});
        inactiveVibe.deactivate();
        vibeRepository.saveAndFlush(inactiveVibe);

        mockMvc.perform(get("/api/v1/catalog/providers")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(netflix.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Netflix"));

        mockMvc.perform(get("/api/v1/catalog/vibes")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(funny.getId().toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(inactiveVibe.getId().toString()))));
    }

    @Test
    void shouldRequireAuthenticationForTheNewEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private UserAccountEntity newUser(String email) {
        return new UserAccountEntity(
                email,
                "password-hash",
                "Pessoa",
                "America/Sao_Paulo",
                "BR"
        );
    }

    private MovieCacheEntity newMovie(
            Long tmdbId,
            String title,
            String posterPath,
            BigDecimal rating
    ) {
        var movie = new MovieCacheEntity(tmdbId, title, new Integer[]{18}, Instant.now());
        movie.refreshMetadata(
                title,
                title,
                "Sinopse",
                posterPath,
                LocalDate.of(2020, 1, 1),
                rating,
                1_000,
                new Integer[]{18},
                false,
                "pt",
                120,
                Instant.now()
        );
        return movie;
    }

    private String bearerToken(UserAccountEntity user) {
        return "Bearer " + jwtService.generateToken(user.getId());
    }
}
