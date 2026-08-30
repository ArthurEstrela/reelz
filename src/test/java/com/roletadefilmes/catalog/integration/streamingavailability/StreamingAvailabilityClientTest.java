package com.roletadefilmes.catalog.integration.streamingavailability;

import com.roletadefilmes.streaming.domain.MonetizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StreamingAvailabilityClientTest {

    private MockRestServiceServer server;
    private StreamingAvailabilityClient client;

    @BeforeEach
    void setUp() {
        var properties = properties();
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-API-Key", properties.apiKey())
                .build();
        client = new StreamingAvailabilityClient(restClient, properties);
    }

    @Test
    void shouldLoadBrazilianProvidersWithAbsoluteLogoUrls() {
        server.expect(requestTo(containsString("/countries/br")))
                .andExpect(requestTo(containsString("output_language=en")))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andRespond(withSuccess("""
                        {
                          "countryCode": "br",
                          "name": "Brazil",
                          "services": [{
                            "id": "netflix",
                            "name": "Netflix",
                            "imageSet": {
                              "lightThemeImage": "https://cdn.example/netflix-light.png",
                              "darkThemeImage": "https://cdn.example/netflix-dark.png",
                              "whiteImage": "https://cdn.example/netflix-white.png"
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var providers = client.listProviders();

        assertThat(providers).singleElement().satisfies(provider -> {
            assertThat(provider.serviceId()).isEqualTo("netflix");
            assertThat(provider.name()).isEqualTo("Netflix");
            assertThat(provider.logoUrl()).endsWith("netflix-dark.png");
            assertThat(provider.displayPriority()).isZero();
        });
        server.verify();
    }

    @Test
    void shouldMapSearchPageMetadataGenresRatingAndDeepLinks() {
        server.expect(requestTo(containsString("/shows/search/filters")))
                .andExpect(requestTo(containsString("country=BR".toLowerCase())))
                .andExpect(requestTo(containsString("show_type=movie")))
                .andExpect(requestTo(containsString("cursor=next-page")))
                .andRespond(withSuccess(searchResponse(), MediaType.APPLICATION_JSON));

        var page = client.searchMovies(List.of("netflix.subscription"), "next-page");

        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isEqualTo("cursor-2");
        assertThat(page.movies()).singleElement().satisfies(movie -> {
            assertThat(movie.externalId()).isEqualTo("movie-550");
            assertThat(movie.tmdbId()).isEqualTo(550L);
            assertThat(movie.voteAverage()).isEqualByComparingTo("8.4");
            assertThat(movie.genreIds()).containsExactly(18, 53);
            assertThat(movie.posterUrl()).isEqualTo("https://cdn.example/fight-club-w480.jpg");
            assertThat(movie.offers()).singleElement().satisfies(offer -> {
                assertThat(offer.serviceId()).isEqualTo("netflix");
                assertThat(offer.monetizationType()).isEqualTo(MonetizationType.FLATRATE);
                assertThat(offer.deepLink()).isEqualTo("https://www.netflix.com/watch/550");
                assertThat(offer.availableFrom()).isEqualTo(Instant.ofEpochSecond(1_725_000_000L));
            });
        });
        server.verify();
    }

    @Test
    void shouldMapShowsIncludedInIncrementalChanges() {
        server.expect(requestTo(containsString("/changes")))
                .andExpect(requestTo(containsString("change_type=updated")))
                .andExpect(requestTo(containsString("item_type=show")))
                .andExpect(requestTo(containsString("from=1725000000")))
                .andExpect(requestTo(containsString("to=1725086400")))
                .andRespond(withSuccess("""
                        {
                          "changes": [{"showId": "movie-550", "changeType": "updated"}],
                          "shows": {"movie-550": %s},
                          "hasMore": false
                        }
                        """.formatted(showResponse()), MediaType.APPLICATION_JSON));

        var page = client.listUpdatedMovies(
                List.of("netflix.subscription"),
                Instant.ofEpochSecond(1_725_000_000L),
                Instant.ofEpochSecond(1_725_086_400L),
                null
        );

        assertThat(page.hasMore()).isFalse();
        assertThat(page.movies()).singleElement()
                .extracting(StreamingAvailabilityMovieData::tmdbId)
                .isEqualTo(550L);
        server.verify();
    }

    private String searchResponse() {
        return """
                {
                  "shows": [%s],
                  "hasMore": true,
                  "nextCursor": "cursor-2"
                }
                """.formatted(showResponse());
    }

    private String showResponse() {
        return """
                {
                  "id": "movie-550",
                  "imdbId": "tt0137523",
                  "tmdbId": "movie/550",
                  "title": "Fight Club",
                  "originalTitle": "Fight Club",
                  "overview": "An insomniac meets a soap maker.",
                  "releaseYear": 1999,
                  "rating": 84,
                  "runtime": 139,
                  "genres": [
                    {"id": "drama", "name": "Drama"},
                    {"id": "thriller", "name": "Thriller"}
                  ],
                  "imageSet": {
                    "verticalPoster": {
                      "w240": "https://cdn.example/fight-club-w240.jpg",
                      "w360": "https://cdn.example/fight-club-w360.jpg",
                      "w480": "https://cdn.example/fight-club-w480.jpg",
                      "w600": "https://cdn.example/fight-club-w600.jpg",
                      "w720": "https://cdn.example/fight-club-w720.jpg"
                    }
                  },
                  "streamingOptions": {
                    "br": [{
                      "service": {
                        "id": "netflix",
                        "name": "Netflix",
                        "imageSet": {
                          "lightThemeImage": "https://cdn.example/netflix-light.png",
                          "darkThemeImage": "https://cdn.example/netflix-dark.png",
                          "whiteImage": "https://cdn.example/netflix-white.png"
                        }
                      },
                      "type": "subscription",
                      "link": "https://www.netflix.com/title/550",
                      "videoLink": "https://www.netflix.com/watch/550",
                      "availableSince": 1725000000,
                      "expiresSoon": false
                    }]
                  }
                }
                """;
    }

    private StreamingAvailabilityProperties properties() {
        return new StreamingAvailabilityProperties(
                true,
                "test-api-key",
                "https://api.movieofthenight.com/v4",
                "BR",
                "en",
                List.of("netflix.subscription", "prime.subscription"),
                20,
                10,
                false,
                false,
                false,
                "0 0 4 * * *",
                "America/Sao_Paulo",
                Duration.ofMinutes(30),
                2,
                Duration.ZERO,
                Duration.ofSeconds(5),
                Duration.ofSeconds(20)
        );
    }
}
