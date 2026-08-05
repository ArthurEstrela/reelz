package com.roletadefilmes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.analytics.persistence.repository.ProductEventRepository;
import com.roletadefilmes.feedback.persistence.repository.BetaFeedbackRepository;
import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.persistence.entity.UserMovieHistoryEntity;
import com.roletadefilmes.history.persistence.repository.UserMovieHistoryRepository;
import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.roulette.persistence.entity.RouletteDailyUsageEntity;
import com.roletadefilmes.roulette.persistence.repository.RouletteDailyUsageRepository;
import com.roletadefilmes.security.JwtService;
import com.roletadefilmes.social.persistence.repository.SocialRoomSpinRepository;
import com.roletadefilmes.streaming.domain.MonetizationType;
import com.roletadefilmes.streaming.persistence.entity.MovieStreamingOfferEntity;
import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import com.roletadefilmes.streaming.persistence.entity.UserStreamingPreferenceEntity;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.streaming.persistence.repository.UserStreamingPreferenceRepository;
import com.roletadefilmes.user.persistence.entity.UserAccountEntity;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import com.roletadefilmes.user.domain.UserRole;
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
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.jpa.open-in-view=false",
        "management.prometheus.metrics.export.enabled=true",
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
    private ObjectMapper objectMapper;

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
    private UserStreamingPreferenceRepository streamingPreferenceRepository;

    @Autowired
    private VibeRepository vibeRepository;

    @Autowired
    private ProductEventRepository productEventRepository;

    @Autowired
    private BetaFeedbackRepository feedbackRepository;

    @Autowired
    private SocialRoomSpinRepository socialRoomSpinRepository;

    @Test
    void shouldCreateJoinAndSpinACoupleRoomExcludingEveryMembersHistory() throws Exception {
        var host = userRepository.saveAndFlush(newUser("social-host@reelz.app"));
        var guest = userRepository.saveAndFlush(newUser("social-guest@reelz.app"));
        var thirdUser = newUser("social-third@reelz.app");
        thirdUser.promoteToAdmin();
        userRepository.saveAndFlush(thirdUser);
        var provider = providerRepository.saveAndFlush(new StreamingProviderEntity(8, "Netflix"));
        var watchedByGuest = movieRepository.saveAndFlush(newMovie(
                31_001L,
                "Já visto pelo convidado",
                "/watched-social.jpg",
                new BigDecimal("8.0")
        ));
        var eligible = movieRepository.saveAndFlush(newMovie(
                31_002L,
                "Escolha do casal",
                "/eligible-social.jpg",
                new BigDecimal("7.8")
        ));
        offerRepository.saveAllAndFlush(List.of(
                new MovieStreamingOfferEntity(
                        watchedByGuest,
                        provider,
                        "BR",
                        MonetizationType.FLATRATE,
                        Instant.now()
                ),
                new MovieStreamingOfferEntity(
                        eligible,
                        provider,
                        "BR",
                        MonetizationType.FLATRATE,
                        Instant.now()
                )
        ));
        streamingPreferenceRepository.saveAllAndFlush(List.of(
                new UserStreamingPreferenceEntity(host, provider),
                new UserStreamingPreferenceEntity(guest, provider),
                new UserStreamingPreferenceEntity(thirdUser, provider)
        ));
        historyRepository.saveAndFlush(new UserMovieHistoryEntity(
                guest,
                watchedByGuest,
                UserMovieStatus.WATCHED,
                Instant.now(),
                null
        ));

        var createResult = mockMvc.perform(post("/api/v1/social/rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"COUPLE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentUserHost").value(true))
                .andExpect(jsonPath("$.capacity").value(2))
                .andReturn();
        var createdRoom = objectMapper.readTree(createResult.getResponse().getContentAsByteArray());
        var roomId = createdRoom.get("id").asText();
        var inviteCode = createdRoom.get("inviteCode").asText();

        mockMvc.perform(post("/api/v1/social/rooms/join")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"%s"}
                                """.formatted(inviteCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.commonProviders[0].id").value(provider.getId().toString()));

        mockMvc.perform(post("/api/v1/social/rooms/join")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(thirdUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"%s"}
                                """.formatted(inviteCode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOCIAL_ROOM_CONFLICT"));

        var spinBody = """
                {
                  "idempotencyKey":"%s",
                  "providerIds":["%s"]
                }
                """.formatted(UUID.randomUUID(), provider.getId());
        mockMvc.perform(post("/api/v1/social/rooms/{roomId}/spin", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spinBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOCIAL_ROOM_ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/social/rooms/{roomId}/spin", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spinBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie.tmdbId").value(eligible.getTmdbId()))
                .andExpect(jsonPath("$.room.lastSpinNumber").value(1))
                .andExpect(jsonPath("$.quota.remainingDailySpins").value(4));

        mockMvc.perform(post("/api/v1/social/rooms/{roomId}/spin", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spinBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie.tmdbId").value(eligible.getTmdbId()))
                .andExpect(jsonPath("$.room.lastSpinNumber").value(1))
                .andExpect(jsonPath("$.quota.remainingDailySpins").value(4));

        mockMvc.perform(get("/api/v1/social/rooms/{roomId}", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastMovie.tmdbId").value(eligible.getTmdbId()))
                .andExpect(jsonPath("$.lastSpinNumber").value(1));

        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .param("days", "30")
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken(thirdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socialRoomsCreated").value(1))
                .andExpect(jsonPath("$.socialRoomsWithSpin").value(1))
                .andExpect(jsonPath("$.socialSpins").value(1))
                .andExpect(jsonPath("$.socialParticipants").value(2));

        assertThat(socialRoomSpinRepository.count()).isEqualTo(1);
        assertThat(dailyUsageRepository.findAll()).hasSize(1);
        assertThat(dailyUsageRepository.findAll().getFirst().getUser().getId()).isEqualTo(host.getId());
    }

    @Test
    void shouldTrackIdempotentProductEventsAndExposeOnlyAggregatesToAdmins() throws Exception {
        var user = userRepository.saveAndFlush(newUser("analytics-user@reelz.app"));
        var admin = newUser("analytics-admin@reelz.app");
        admin.promoteToAdmin();
        userRepository.saveAndFlush(admin);
        var eventId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var eventBody = """
                {
                  "eventId":"%s",
                  "sessionId":"%s",
                  "eventType":"COUPLE_MODE_INTERESTED"
                }
                """.formatted(eventId, sessionId);

        mockMvc.perform(post("/api/v1/analytics/events")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/analytics/events")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody))
                .andExpect(status().isAccepted());

        assertThat(productEventRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/feedback")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":5,"message":"Decidi muito mais rápido."}
                                """))
                .andExpect(status().isAccepted());

        assertThat(feedbackRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/feedback")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":6,"message":"fora da escala"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(feedbackRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .param("days", "30")
                        .header(HttpHeaders.AUTHORIZATION, adminBearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.coupleModeInterestedUsers").value(1))
                .andExpect(jsonPath("$.feedbackCount").value(1))
                .andExpect(jsonPath("$.averageFeedbackScore").value(5.0))
                .andExpect(jsonPath("$.recentFeedback[0].message")
                        .value("Decidi muito mais rápido."))
                .andExpect(jsonPath("$.daily").isArray());
    }

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
    void shouldListAndRemoveWatchlistItemsWithoutDeletingWatchedHistory() throws Exception {
        var user = userRepository.saveAndFlush(newUser("watchlist-api@reelz.app"));
        var savedForLater = movieRepository.saveAndFlush(newMovie(
                401L,
                "Para ver depois",
                "/later.jpg",
                new BigDecimal("7.8")
        ));
        var alreadyWatched = movieRepository.saveAndFlush(newMovie(
                402L,
                "Já assistido",
                "/watched.jpg",
                new BigDecimal("8.1")
        ));
        historyRepository.saveAllAndFlush(List.of(
                new UserMovieHistoryEntity(
                        user,
                        savedForLater,
                        UserMovieStatus.WATCHLIST,
                        null,
                        null
                ),
                new UserMovieHistoryEntity(
                        user,
                        alreadyWatched,
                        UserMovieStatus.WATCHED,
                        Instant.now(),
                        null
                )
        ));

        mockMvc.perform(get("/api/v1/history?status=WATCHLIST&page=0&size=24")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].movieId").value(401))
                .andExpect(jsonPath("$.content[0].status").value("WATCHLIST"))
                .andExpect(jsonPath("$.content[0].watchedAt").value(nullValue()));

        mockMvc.perform(delete("/api/v1/history/watchlist/{movieId}", 401L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/history/watchlist/{movieId}", 402L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isNoContent());

        assertThat(historyRepository.findByUserIdAndMovieId(user.getId(), savedForLater.getId()))
                .isEmpty();
        assertThat(historyRepository.findByUserIdAndMovieId(user.getId(), alreadyWatched.getId()))
                .isPresent()
                .get()
                .extracting(UserMovieHistoryEntity::getStatus)
                .isEqualTo(UserMovieStatus.WATCHED);
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
    void shouldReplaceAndReturnStreamingPreferencesWithoutApplyingTheFreeSpinLimit() throws Exception {
        var user = userRepository.saveAndFlush(newUser("preferences-api@reelz.app"));
        var netflix = new StreamingProviderEntity(8, "Netflix");
        netflix.refreshCatalogData("Netflix", null, 0);
        netflix = providerRepository.saveAndFlush(netflix);
        var max = new StreamingProviderEntity(1899, "HBO Max");
        max.refreshCatalogData("HBO Max", null, 1);
        max = providerRepository.saveAndFlush(max);
        var unavailableProvider = providerRepository.saveAndFlush(
                new StreamingProviderEntity(9999, "Indisponível")
        );
        var movie = movieRepository.saveAndFlush(newMovie(
                9100L,
                "Filme compartilhado",
                "/shared.jpg",
                new BigDecimal("7.5")
        ));
        offerRepository.saveAllAndFlush(List.of(
                new MovieStreamingOfferEntity(movie, netflix, "BR", MonetizationType.FLATRATE, Instant.now()),
                new MovieStreamingOfferEntity(movie, max, "BR", MonetizationType.FLATRATE, Instant.now())
        ));

        mockMvc.perform(get("/api/v1/users/me/streaming-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerIds.length()").value(0));

        mockMvc.perform(put("/api/v1/users/me/streaming-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerIds":["%s","%s"]}
                                """.formatted(max.getId(), netflix.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerIds[0]").value(netflix.getId().toString()))
                .andExpect(jsonPath("$.providerIds[1]").value(max.getId().toString()));

        mockMvc.perform(get("/api/v1/users/me/streaming-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerIds.length()").value(2));
        assertThat(streamingPreferenceRepository.findAllByUserId(user.getId())).hasSize(2);

        mockMvc.perform(put("/api/v1/users/me/streaming-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerIds":["%s"]}
                                """.formatted(unavailableProvider.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STREAMING_PREFERENCE"));
        assertThat(streamingPreferenceRepository.findAllByUserId(user.getId())).hasSize(2);
    }

    @Test
    void shouldServePopularMoviesAndCompleteOnboardingAtomically() throws Exception {
        var user = userRepository.saveAndFlush(newUser("onboarding-api@reelz.app"));
        var provider = providerRepository.saveAndFlush(new StreamingProviderEntity(8, "Netflix"));
        var movies = IntStream.rangeClosed(1, 25)
                .mapToObj(index -> newMovie(
                        20_000L + index,
                        "Filme popular " + index,
                        "/popular-" + index + ".jpg",
                        new BigDecimal("7.5")
                ))
                .toList();
        movieRepository.saveAllAndFlush(movies);
        offerRepository.saveAllAndFlush(movies.stream()
                .map(movie -> new MovieStreamingOfferEntity(
                        movie,
                        provider,
                        "BR",
                        MonetizationType.FLATRATE,
                        Instant.now()
                ))
                .toList());

        mockMvc.perform(get("/api/v1/onboarding/movies?limit=25")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCount").value(25))
                .andExpect(jsonPath("$.movies.length()").value(25))
                .andExpect(jsonPath("$.movies[0].posterPath").isNotEmpty());

        var firstMovieId = movies.get(0).getTmdbId();
        var secondMovieId = movies.get(1).getTmdbId();
        var presentedIds = movies.stream()
                .map(MovieCacheEntity::getTmdbId)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "presentedMovieIds":[%s],
                                  "watchedMovieIds":[%d,%d]
                                }
                                """.formatted(presentedIds, firstMovieId, secondMovieId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.watchedMoviesAdded").value(2));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getOnboardingCompletedAt())
                .isNotNull();
        assertThat(historyRepository.findAll())
                .extracting(history -> history.getMovie().getTmdbId())
                .containsExactlyInAnyOrder(firstMovieId, secondMovieId);
    }

    @Test
    void shouldRequireAuthenticationForTheNewEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldExposeHealthProbesAndPrometheusMetricsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
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

    private String adminBearerToken(UserAccountEntity user) {
        return "Bearer " + jwtService.generateToken(user.getId(), UserRole.ADMIN);
    }
}
