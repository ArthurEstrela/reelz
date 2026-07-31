package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.integration.tmdb.TmdbAvailability;
import com.roletadefilmes.catalog.integration.tmdb.TmdbMovieData;
import com.roletadefilmes.catalog.integration.tmdb.TmdbOfferData;
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
import java.util.Map;
import java.util.Set;

@Service
public class TmdbCatalogPersistenceService {

    private final MovieCacheRepository movieRepository;
    private final StreamingProviderRepository providerRepository;
    private final MovieStreamingOfferRepository offerRepository;

    public TmdbCatalogPersistenceService(
            MovieCacheRepository movieRepository,
            StreamingProviderRepository providerRepository,
            MovieStreamingOfferRepository offerRepository
    ) {
        this.movieRepository = movieRepository;
        this.providerRepository = providerRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public int upsert(
            TmdbMovieData movieData,
            TmdbAvailability availability,
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
                movieData.posterPath(),
                movieData.releaseDate(),
                movieData.voteAverage(),
                movieData.voteCount(),
                movieData.genreIds().toArray(Integer[]::new),
                movieData.adult(),
                movieData.originalLanguage(),
                null,
                syncedAt
        );
        movie = movieRepository.save(movie);

        Map<OfferKey, MovieStreamingOfferEntity> existingOffers = new HashMap<>();
        for (var offer : offerRepository.findAllByMovieIdAndCountryCode(movie.getId(), countryCode)) {
            existingOffers.put(
                    new OfferKey(offer.getProvider().getTmdbProviderId(), offer.getMonetizationType()),
                    offer
            );
        }

        Set<OfferKey> synchronizedOffers = new HashSet<>();
        for (TmdbOfferData offerData : availability.offers()) {
            var provider = upsertProvider(offerData);
            var key = new OfferKey(offerData.providerId(), offerData.monetizationType());
            var offer = existingOffers.get(key);
            if (offer == null) {
                offer = new MovieStreamingOfferEntity(
                        movie,
                        provider,
                        countryCode,
                        offerData.monetizationType(),
                        syncedAt
                );
            }
            offer.refreshAvailability(availability.attributionUrl(), null, null, syncedAt);
            offerRepository.save(offer);
            synchronizedOffers.add(key);
        }

        var staleOffers = existingOffers.entrySet().stream()
                .filter(entry -> !synchronizedOffers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        offerRepository.deleteAll(staleOffers);
        return synchronizedOffers.size();
    }

    @Transactional
    public void activateOnly(Set<Integer> activeTmdbProviderIds) {
        for (var provider : providerRepository.findAll()) {
            if (activeTmdbProviderIds.contains(provider.getTmdbProviderId())) {
                provider.activate();
            } else {
                provider.deactivate();
            }
        }
    }

    private StreamingProviderEntity upsertProvider(TmdbOfferData offerData) {
        var provider = providerRepository.findByTmdbProviderId(offerData.providerId())
                .orElseGet(() -> new StreamingProviderEntity(
                        offerData.providerId(),
                        offerData.providerName()
                ));
        provider.refreshCatalogData(
                offerData.providerName(),
                offerData.logoPath(),
                offerData.displayPriority()
        );
        return providerRepository.save(provider);
    }

    private record OfferKey(int providerId, MonetizationType monetizationType) {
    }
}
