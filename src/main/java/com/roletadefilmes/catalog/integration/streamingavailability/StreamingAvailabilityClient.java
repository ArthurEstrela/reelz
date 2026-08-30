package com.roletadefilmes.catalog.integration.streamingavailability;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.roletadefilmes.streaming.domain.MonetizationType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class StreamingAvailabilityClient {

    private final RestClient restClient;
    private final StreamingAvailabilityProperties properties;

    public StreamingAvailabilityClient(
            @Qualifier("streamingAvailabilityRestClient") RestClient restClient,
            StreamingAvailabilityProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<StreamingAvailabilityProviderData> listProviders() {
        requireConfiguration();
        var response = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/countries/{country}")
                        .queryParam("output_language", properties.outputLanguage())
                        .build(properties.country().toLowerCase()))
                .retrieve()
                .body(CountryResponse.class));

        if (response == null || response.services() == null) {
            return List.of();
        }

        List<StreamingAvailabilityProviderData> providers = new ArrayList<>();
        for (int index = 0; index < response.services().size(); index++) {
            var service = response.services().get(index);
            if (service == null || !StringUtils.hasText(service.id()) || !StringUtils.hasText(service.name())) {
                continue;
            }
            providers.add(new StreamingAvailabilityProviderData(
                    service.id(),
                    service.name(),
                    selectProviderLogo(service.imageSet()),
                    index
            ));
        }
        return List.copyOf(providers);
    }

    public StreamingAvailabilityMoviePage searchMovies(List<String> catalogs, String cursor) {
        requireConfiguration();
        requireCatalogs(catalogs);
        var response = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/shows/search/filters")
                        .queryParam("country", properties.country().toLowerCase())
                        .queryParam("catalogs", String.join(",", catalogs))
                        .queryParam("output_language", properties.outputLanguage())
                        .queryParam("show_type", "movie")
                        .queryParam("order_by", "popularity_alltime")
                        .queryParam("order_direction", "desc")
                        .queryParamIfPresent("cursor", Optional.ofNullable(cursor))
                        .build())
                .retrieve()
                .body(SearchResponse.class));
        return toMoviePage(response);
    }

    public StreamingAvailabilityMoviePage listUpdatedMovies(
            List<String> catalogs,
            Instant from,
            Instant to,
            String cursor
    ) {
        requireConfiguration();
        requireCatalogs(catalogs);
        var response = executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/changes")
                        .queryParam("country", properties.country().toLowerCase())
                        .queryParam("catalogs", String.join(",", catalogs))
                        .queryParam("change_type", "updated")
                        .queryParam("item_type", "show")
                        .queryParam("show_type", "movie")
                        .queryParam("from", from.getEpochSecond())
                        .queryParam("to", to.getEpochSecond())
                        .queryParam("order_direction", "asc")
                        .queryParam("output_language", properties.outputLanguage())
                        .queryParamIfPresent("cursor", Optional.ofNullable(cursor))
                        .build())
                .retrieve()
                .body(ChangesResponse.class));

        if (response == null) {
            return new StreamingAvailabilityMoviePage(List.of(), false, null);
        }
        var shows = response.shows() == null ? List.<ShowResponse>of() : response.shows().values().stream().toList();
        return new StreamingAvailabilityMoviePage(
                shows.stream().map(this::toMovieData).flatMap(Optional::stream).toList(),
                response.hasMore(),
                response.nextCursor()
        );
    }

    private StreamingAvailabilityMoviePage toMoviePage(SearchResponse response) {
        if (response == null || response.shows() == null) {
            return new StreamingAvailabilityMoviePage(List.of(), false, null);
        }
        return new StreamingAvailabilityMoviePage(
                response.shows().stream().map(this::toMovieData).flatMap(Optional::stream).toList(),
                response.hasMore(),
                response.nextCursor()
        );
    }

    private Optional<StreamingAvailabilityMovieData> toMovieData(ShowResponse show) {
        if (show == null || !StringUtils.hasText(show.id()) || !StringUtils.hasText(show.title())) {
            return Optional.empty();
        }

        Long tmdbId = parseTmdbMovieId(show.tmdbId());
        if (tmdbId == null) {
            return Optional.empty();
        }

        var genres = show.genres() == null ? List.<Integer>of() : show.genres().stream()
                .filter(Objects::nonNull)
                .map(GenreResponse::id)
                .map(StreamingAvailabilityGenreMapping::toTmdbId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return Optional.of(new StreamingAvailabilityMovieData(
                show.id(),
                tmdbId,
                show.imdbId(),
                show.title(),
                show.originalTitle(),
                show.overview(),
                selectPoster(show.imageSet()),
                toReleaseDate(show.releaseYear()),
                normalizeRating(show.rating()),
                genres,
                positiveOrNull(show.runtime()),
                mapOffers(show.streamingOptions())
        ));
    }

    private Long parseTmdbMovieId(String rawTmdbId) {
        if (!StringUtils.hasText(rawTmdbId)) {
            return null;
        }
        String normalized = rawTmdbId.trim();
        if (normalized.startsWith("movie/")) {
            normalized = normalized.substring("movie/".length());
        } else if (normalized.contains("/")) {
            return null;
        }
        try {
            long parsed = Long.parseLong(normalized);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<StreamingAvailabilityOfferData> mapOffers(Map<String, List<StreamingOptionResponse>> byCountry) {
        if (byCountry == null || byCountry.isEmpty()) {
            return List.of();
        }
        String configuredCountry = properties.country();
        var options = byCountry.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(configuredCountry))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(List.of());

        return options.stream()
                .filter(Objects::nonNull)
                .map(this::toOfferData)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<StreamingAvailabilityOfferData> toOfferData(StreamingOptionResponse option) {
        if (option.service() == null || !StringUtils.hasText(option.service().id())) {
            return Optional.empty();
        }
        var monetizationType = toMonetizationType(option.type());
        if (monetizationType == null) {
            return Optional.empty();
        }
        return Optional.of(new StreamingAvailabilityOfferData(
                option.service().id(),
                option.service().name(),
                selectProviderLogo(option.service().imageSet()),
                monetizationType,
                option.videoLink() != null ? option.videoLink() : option.link(),
                toInstant(option.availableSince()),
                toInstant(option.expiresOn())
        ));
    }

    private MonetizationType toMonetizationType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "subscription" -> MonetizationType.FLATRATE;
            case "free" -> MonetizationType.FREE;
            case "rent" -> MonetizationType.RENT;
            case "buy" -> MonetizationType.BUY;
            default -> null;
        };
    }

    private BigDecimal normalizeRating(Integer rating) {
        if (rating == null || rating < 0 || rating > 100) {
            return null;
        }
        return BigDecimal.valueOf(rating)
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
    }

    private LocalDate toReleaseDate(Integer releaseYear) {
        if (releaseYear == null || releaseYear < 1870 || releaseYear > 3000) {
            return null;
        }
        return LocalDate.of(releaseYear, 1, 1);
    }

    private Integer positiveOrNull(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private Instant toInstant(Long epochSeconds) {
        return epochSeconds == null || epochSeconds <= 0 ? null : Instant.ofEpochSecond(epochSeconds);
    }

    private String selectPoster(ShowImageSetResponse imageSet) {
        if (imageSet == null || imageSet.verticalPoster() == null) {
            return null;
        }
        var poster = imageSet.verticalPoster();
        return firstText(poster.w480(), poster.w360(), poster.w240(), poster.w600(), poster.w720());
    }

    private String selectProviderLogo(ServiceImageSetResponse imageSet) {
        if (imageSet == null) {
            return null;
        }
        return firstText(imageSet.darkThemeImage(), imageSet.whiteImage(), imageSet.lightThemeImage());
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void requireConfiguration() {
        if (!properties.enabled()) {
            throw new IllegalStateException("Streaming Availability synchronization is disabled");
        }
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("STREAMING_AVAILABILITY_API_KEY is required for catalog synchronization");
        }
        if (properties.catalogs().isEmpty()) {
            throw new IllegalStateException("At least one STREAMING_AVAILABILITY_CATALOGS entry is required");
        }
    }

    private void requireCatalogs(List<String> catalogs) {
        if (catalogs == null || catalogs.isEmpty()) {
            throw new IllegalArgumentException("At least one validated Streaming Availability catalog is required");
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
                    // HTTP-date is ignored; the configured bounded backoff still applies.
                }
            }
        }
        delayMillis = Math.min(delayMillis, 5_000L);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RestClientException(
                    "Interrupted while retrying a Streaming Availability request",
                    interruptedException
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CountryResponse(String countryCode, String name, List<ServiceResponse> services) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ServiceResponse(String id, String name, ServiceImageSetResponse imageSet) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ServiceImageSetResponse(
            String lightThemeImage,
            String darkThemeImage,
            String whiteImage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(List<ShowResponse> shows, boolean hasMore, String nextCursor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChangesResponse(
            List<ChangeResponse> changes,
            Map<String, ShowResponse> shows,
            boolean hasMore,
            String nextCursor
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChangeResponse(String showId, String changeType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ShowResponse(
            String id,
            String imdbId,
            String tmdbId,
            String title,
            String originalTitle,
            String overview,
            Integer releaseYear,
            Integer rating,
            Integer runtime,
            List<GenreResponse> genres,
            ShowImageSetResponse imageSet,
            Map<String, List<StreamingOptionResponse>> streamingOptions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GenreResponse(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ShowImageSetResponse(VerticalImageResponse verticalPoster) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VerticalImageResponse(
            String w240,
            String w360,
            String w480,
            String w600,
            String w720
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StreamingOptionResponse(
            ServiceResponse service,
            String type,
            String link,
            String videoLink,
            Long availableSince,
            Long expiresOn
    ) {
    }
}
