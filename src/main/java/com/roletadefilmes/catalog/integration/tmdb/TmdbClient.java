package com.roletadefilmes.catalog.integration.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.roletadefilmes.streaming.domain.MonetizationType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbClient(RestClient tmdbRestClient, TmdbProperties properties) {
        this.restClient = tmdbRestClient;
        this.properties = properties;
    }

    public TmdbDiscoverPage discoverMovies(int page) {
        return discoverMovies(null, page);
    }

    public TmdbDiscoverPage discoverMovies(int providerId, int page) {
        return discoverMovies(Integer.valueOf(providerId), page);
    }

    public List<TmdbProviderData> listMovieProviders() {
        requireToken();
        var response = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/watch/providers/movie")
                        .queryParam("language", properties.language())
                        .queryParam("watch_region", properties.region())
                        .build())
                .retrieve()
                .body(ProviderListResponse.class));

        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .filter(provider -> provider.providerId() > 0 && StringUtils.hasText(provider.providerName()))
                .map(provider -> new TmdbProviderData(
                        provider.providerId(),
                        provider.providerName(),
                        provider.logoPath(),
                        Math.max(0, provider.displayPriority())
                ))
                .toList();
    }

    private TmdbDiscoverPage discoverMovies(Integer providerId, int page) {
        requireToken();
        var response = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("include_adult", false)
                        .queryParam("include_video", false)
                        .queryParam("language", properties.language())
                        .queryParam("page", page)
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("watch_region", properties.region())
                        .queryParam("with_watch_monetization_types", "flatrate|free|ads")
                        .queryParamIfPresent("with_watch_providers", java.util.Optional.ofNullable(providerId))
                        .build())
                .retrieve()
                .body(DiscoverResponse.class));

        if (response == null || response.results() == null) {
            return new TmdbDiscoverPage(List.of(), 0);
        }

        var movies = response.results().stream()
                .filter(movie -> movie.id() > 0 && StringUtils.hasText(movie.title()))
                .map(this::toMovieData)
                .toList();
        return new TmdbDiscoverPage(movies, response.totalPages());
    }

    public TmdbAvailability findAvailability(long tmdbMovieId) {
        requireToken();
        var response = executeWithRetry(() -> restClient.get()
                .uri("/movie/{movieId}/watch/providers", tmdbMovieId)
                .retrieve()
                .body(WatchProvidersResponse.class));

        if (response == null || response.results() == null) {
            return TmdbAvailability.empty();
        }

        var region = response.results().get(properties.region().toUpperCase());
        if (region == null) {
            return TmdbAvailability.empty();
        }

        List<TmdbOfferData> offers = new ArrayList<>();
        appendOffers(offers, region.flatrate(), MonetizationType.FLATRATE);
        appendOffers(offers, region.free(), MonetizationType.FREE);
        appendOffers(offers, region.ads(), MonetizationType.ADS);
        appendOffers(offers, region.rent(), MonetizationType.RENT);
        appendOffers(offers, region.buy(), MonetizationType.BUY);
        return new TmdbAvailability(region.link(), List.copyOf(offers));
    }

    private TmdbMovieData toMovieData(MovieResult movie) {
        return new TmdbMovieData(
                movie.id(),
                movie.title(),
                movie.originalTitle(),
                movie.overview(),
                movie.posterPath(),
                parseDate(movie.releaseDate()),
                movie.voteAverage(),
                Math.max(0, movie.voteCount()),
                movie.genreIds() == null ? List.of() : List.copyOf(movie.genreIds()),
                movie.adult(),
                movie.originalLanguage()
        );
    }

    private void appendOffers(
            List<TmdbOfferData> target,
            List<ProviderResult> providers,
            MonetizationType monetizationType
    ) {
        if (providers == null) {
            return;
        }
        providers.stream()
                .filter(provider -> provider.providerId() > 0 && StringUtils.hasText(provider.providerName()))
                .map(provider -> new TmdbOfferData(
                        provider.providerId(),
                        provider.providerName(),
                        provider.logoPath(),
                        Math.max(0, provider.displayPriority()),
                        monetizationType
                ))
                .forEach(target::add);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void requireToken() {
        if (!StringUtils.hasText(properties.readAccessToken())) {
            throw new IllegalStateException("TMDB_READ_ACCESS_TOKEN is required for catalog synchronization");
        }
    }

    private <T> T executeWithRetry(Supplier<T> request) {
        int attempts = Math.max(1, properties.maxAttempts());
        RestClientException lastFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return request.get();
            } catch (RestClientException exception) {
                lastFailure = exception;
                if (attempt == attempts || !isRetryable(exception)) {
                    throw exception;
                }
                waitBeforeRetry(exception, attempt);
            }
        }
        throw lastFailure;
    }

    private boolean isRetryable(RestClientException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        return exception instanceof RestClientResponseException responseException
                && (responseException.getStatusCode().value() == 429
                || responseException.getStatusCode().is5xxServerError());
    }

    private void waitBeforeRetry(RestClientException exception, int attempt) {
        long delayMillis = properties.retryBaseDelay().toMillis() * attempt;
        if (exception instanceof RestClientResponseException responseException
                && responseException.getResponseHeaders() != null) {
            String retryAfter = responseException.getResponseHeaders().getFirst("Retry-After");
            if (retryAfter != null) {
                try {
                    delayMillis = Math.max(delayMillis, Long.parseLong(retryAfter) * 1000L);
                } catch (NumberFormatException ignored) {
                    // HTTP-date is intentionally ignored; the configured bounded backoff still applies.
                }
            }
        }
        delayMillis = Math.min(delayMillis, 5_000L);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Interrupted while retrying a TMDB request", interruptedException);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DiscoverResponse(
            @JsonProperty("total_pages") int totalPages,
            List<MovieResult> results
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MovieResult(
            long id,
            String title,
            @JsonProperty("original_title") String originalTitle,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("vote_average") BigDecimal voteAverage,
            @JsonProperty("vote_count") int voteCount,
            @JsonProperty("genre_ids") List<Integer> genreIds,
            boolean adult,
            @JsonProperty("original_language") String originalLanguage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WatchProvidersResponse(Map<String, WatchRegion> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProviderListResponse(List<ProviderResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WatchRegion(
            String link,
            List<ProviderResult> flatrate,
            List<ProviderResult> free,
            List<ProviderResult> ads,
            List<ProviderResult> rent,
            List<ProviderResult> buy
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProviderResult(
            @JsonProperty("provider_id") int providerId,
            @JsonProperty("provider_name") String providerName,
            @JsonProperty("logo_path") String logoPath,
            @JsonProperty("display_priority") int displayPriority
    ) {
    }
}
