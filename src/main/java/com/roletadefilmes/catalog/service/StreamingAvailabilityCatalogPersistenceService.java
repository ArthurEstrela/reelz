package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityMovieData;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityOfferData;
import com.roletadefilmes.catalog.integration.streamingavailability.StreamingAvailabilityProviderData;
import com.roletadefilmes.movie.persistence.entity.MovieCacheEntity;
import com.roletadefilmes.movie.persistence.repository.MovieCacheRepository;
import com.roletadefilmes.streaming.domain.MonetizationType;
import com.roletadefilmes.streaming.persistence.entity.MovieStreamingOfferEntity;
import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import com.roletadefilmes.streaming.persistence.repository.MovieStreamingOfferRepository;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StreamingAvailabilityCatalogPersistenceService {

    private static final String SOURCE = StreamingAvailabilityCatalogProgressService.SOURCE;
    private static final Map<String, Integer> KNOWN_TMDB_PROVIDER_IDS = Map.ofEntries(
            Map.entry("netflix", 8),
            Map.entry("prime", 119),
            Map.entry("apple", 350),
            Map.entry("disney", 337),
            Map.entry("paramount", 531),
            Map.entry("hbo", 1899),
            Map.entry("mubi", 11),
            Map.entry("crunchyroll", 283),
            Map.entry("pluto", 300),
            Map.entry("curiosity", 190),
            Map.entry("zee5", 232)
    );

    private final MovieCacheRepository movieRepository;
    private final StreamingProviderRepository providerRepository;
    private final MovieStreamingOfferRepository offerRepository;

    public StreamingAvailabilityCatalogPersistenceService(
            MovieCacheRepository movieRepository,
            StreamingProviderRepository providerRepository,
            MovieStreamingOfferRepository offerRepository
    ) {
        this.movieRepository = movieRepository;
        this.providerRepository = providerRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public void synchronizeProviders(
            List<StreamingAvailabilityProviderData> providers,
            boolean deactivateUnconfiguredProviders
    ) {
        Set<String> synchronizedIds = new HashSet<>();
        for (var providerData : providers) {
            upsertProvider(providerData);
            synchronizedIds.add(providerData.serviceId());
        }

        if (!deactivateUnconfiguredProviders) {
            return;
        }
        for (var provider : providerRepository.findAll()) {
            if (provider.getStreamingAvailabilityServiceId() != null
                    && synchronizedIds.contains(provider.getStreamingAvailabilityServiceId())) {
                provider.activate();
            } else {
                provider.deactivate();
            }
        }
    }

    @Transactional
    public int upsertMovie(
            StreamingAvailabilityMovieData movieData,
            Set<String> allowedServiceIds,
            String countryCode,
            Instant syncedAt
    ) {
        var movie = movieRepository.findByTmdbId(movieData.tmdbId())
                .orElseGet(() -> new MovieCacheEntity(
                        movieData.tmdbId(),
                        movieData.title(),
                        movieData.genreIds().toArray(Integer[]::new),
                        syncedAt
                ));
        movie.refreshMetadata(
                movieData.title(),
                movieData.originalTitle(),
                movieData.overview(),
                movieData.posterUrl(),
                movieData.releaseDate(),
                movieData.voteAverage(),
                movie.getVoteCount(),
                movieData.genreIds().toArray(Integer[]::new),
                movie.isAdult(),
                movie.getOriginalLanguage(),
                movieData.runtimeMinutes(),
                syncedAt
        );
        movie.identifyExternalMetadata(SOURCE, movieData.externalId(), movieData.imdbId());
        movie = movieRepository.save(movie);

        Map<OfferKey, StreamingAvailabilityOfferData> incomingOffers = new LinkedHashMap<>();
        movieData.offers().stream()
                .filter(offer -> allowedServiceIds.contains(offer.serviceId()))
                .forEach(offer -> incomingOffers.putIfAbsent(
                        new OfferKey(offer.serviceId(), offer.monetizationType()),
                        offer
                ));

        Map<OfferKey, MovieStreamingOfferEntity> existingOffers = new HashMap<>();
        for (var offer : offerRepository.findAllByMovieIdAndCountryCode(movie.getId(), countryCode)) {
            String serviceId = offer.getProvider().getStreamingAvailabilityServiceId();
            if (serviceId != null && allowedServiceIds.contains(serviceId)) {
                existingOffers.put(new OfferKey(serviceId, offer.getMonetizationType()), offer);
            }
        }

        for (var entry : incomingOffers.entrySet()) {
            var offerData = entry.getValue();
            var provider = providerRepository.findByStreamingAvailabilityServiceId(offerData.serviceId())
                    .orElseGet(() -> upsertProvider(new StreamingAvailabilityProviderData(
                            offerData.serviceId(),
                            offerData.providerName(),
                            offerData.logoUrl(),
                            999
                    )));
            var offer = existingOffers.get(entry.getKey());
            if (offer == null) {
                offer = new MovieStreamingOfferEntity(
                        movie,
                        provider,
                        countryCode,
                        offerData.monetizationType(),
                        syncedAt
                );
            }
            offer.refreshAvailability(
                    offerData.deepLink(),
                    offerData.availableFrom(),
                    offerData.availableUntil(),
                    syncedAt,
                    SOURCE
            );
            offerRepository.save(offer);
        }

        var staleOffers = existingOffers.entrySet().stream()
                .filter(entry -> !incomingOffers.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        offerRepository.deleteAll(staleOffers);
        return incomingOffers.size();
    }

    private StreamingProviderEntity upsertProvider(StreamingAvailabilityProviderData providerData) {
        var provider = findProvider(providerData)
                .orElseGet(() -> new StreamingProviderEntity(
                        KNOWN_TMDB_PROVIDER_IDS.get(providerData.serviceId()),
                        providerData.name()
                ));
        provider.linkStreamingAvailabilityService(
                providerData.serviceId(),
                providerData.name(),
                providerData.logoUrl(),
                Math.max(0, providerData.displayPriority())
        );
        provider.activate();
        return providerRepository.save(provider);
    }

    private java.util.Optional<StreamingProviderEntity> findProvider(
            StreamingAvailabilityProviderData providerData
    ) {
        var byServiceId = providerRepository.findByStreamingAvailabilityServiceId(providerData.serviceId());
        if (byServiceId.isPresent()) {
            return byServiceId;
        }
        Integer tmdbProviderId = KNOWN_TMDB_PROVIDER_IDS.get(providerData.serviceId());
        if (tmdbProviderId != null) {
            var byTmdbId = providerRepository.findByTmdbProviderId(tmdbProviderId);
            if (byTmdbId.isPresent()) {
                return byTmdbId;
            }
        }
        return providerRepository.findFirstByNameIgnoreCase(providerData.name());
    }

    private record OfferKey(String serviceId, MonetizationType monetizationType) {
    }
}
