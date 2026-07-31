package com.roletadefilmes.catalog.integration.tmdb;

import com.roletadefilmes.streaming.domain.MonetizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TmdbClientTest {

    private MockRestServiceServer server;
    private TmdbClient client;

    @BeforeEach
    void setUp() {
        var properties = new TmdbProperties(
                "test-token",
                "pt-BR",
                "BR",
                3,
                10,
                List.of(8, 119),
                false,
                false,
                "0 0 4 * * *",
                "America/Sao_Paulo",
                Duration.ofMinutes(30),
                2,
                Duration.ZERO,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var restClient = builder
                .baseUrl("https://api.themoviedb.org/3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.readAccessToken())
                .build();
        client = new TmdbClient(restClient, properties);
    }

    @Test
    void shouldMapDiscoverResultsWithoutLeakingAuthenticationIntoTheContract() {
        server.expect(requestTo(containsString("/discover/movie")))
                .andExpect(requestTo(containsString("with_watch_providers=8")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("""
                        {
                          "total_pages": 12,
                          "results": [{
                            "id": 550,
                            "title": "Clube da Luta",
                            "original_title": "Fight Club",
                            "overview": "Uma sinopse",
                            "poster_path": "/poster.jpg",
                            "release_date": "1999-10-15",
                            "vote_average": 8.4,
                            "vote_count": 30000,
                            "genre_ids": [18, 53],
                            "adult": false,
                            "original_language": "en"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var page = client.discoverMovies(8, 1);

        assertThat(page.totalPages()).isEqualTo(12);
        assertThat(page.movies()).singleElement().satisfies(movie -> {
            assertThat(movie.tmdbId()).isEqualTo(550L);
            assertThat(movie.title()).isEqualTo("Clube da Luta");
            assertThat(movie.genreIds()).containsExactly(18, 53);
        });
        server.verify();
    }

    @Test
    void shouldListMovieProvidersForConfiguredRegionAndRetryRateLimit() {
        server.expect(requestTo(containsString("/watch/providers/movie")))
                .andExpect(requestTo(containsString("watch_region=BR")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(containsString("/watch/providers/movie")))
                .andRespond(withSuccess("""
                        {
                          "results": [{
                            "provider_id": 8,
                            "provider_name": "Netflix",
                            "logo_path": "/netflix.jpg",
                            "display_priority": 0
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var providers = client.listMovieProviders();

        assertThat(providers).singleElement().satisfies(provider -> {
            assertThat(provider.providerId()).isEqualTo(8);
            assertThat(provider.name()).isEqualTo("Netflix");
            assertThat(provider.displayPriority()).isZero();
        });
        server.verify();
    }

    @Test
    void shouldMapBrazilianWatchOffersAndAttributionLink() {
        server.expect(requestTo(containsString("/movie/550/watch/providers")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("""
                        {
                          "results": {
                            "BR": {
                              "link": "https://www.themoviedb.org/movie/550/watch",
                              "flatrate": [{
                                "provider_id": 8,
                                "provider_name": "Netflix",
                                "logo_path": "/netflix.jpg",
                                "display_priority": 1
                              }],
                              "rent": [{
                                "provider_id": 2,
                                "provider_name": "Apple TV",
                                "logo_path": "/apple.jpg",
                                "display_priority": 5
                              }]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var availability = client.findAvailability(550L);

        assertThat(availability.attributionUrl()).contains("/movie/550/watch");
        assertThat(availability.offers()).hasSize(2);
        assertThat(availability.offers())
                .extracting(TmdbOfferData::monetizationType)
                .containsExactly(MonetizationType.FLATRATE, MonetizationType.RENT);
        server.verify();
    }
}
