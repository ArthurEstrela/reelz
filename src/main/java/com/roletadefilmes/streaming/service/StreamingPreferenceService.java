package com.roletadefilmes.streaming.service;

import com.roletadefilmes.streaming.api.dto.StreamingPreferencesResponse;
import com.roletadefilmes.streaming.domain.exception.InvalidStreamingPreferenceException;
import com.roletadefilmes.streaming.persistence.entity.UserStreamingPreferenceEntity;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.streaming.persistence.repository.UserStreamingPreferenceRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class StreamingPreferenceService {

    private final UserAccountRepository userRepository;
    private final StreamingProviderRepository providerRepository;
    private final UserStreamingPreferenceRepository preferenceRepository;

    public StreamingPreferenceService(
            UserAccountRepository userRepository,
            StreamingProviderRepository providerRepository,
            UserStreamingPreferenceRepository preferenceRepository
    ) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public StreamingPreferencesResponse get(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return response(userId);
    }

    @Transactional
    public StreamingPreferencesResponse replace(UUID userId, Set<UUID> requestedProviderIds) {
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Set<UUID> requestedIds = Set.copyOf(requestedProviderIds);
        var eligibleProviders = providerRepository.findEligibleForCountry(user.getCountryCode());
        Set<UUID> eligibleIds = eligibleProviders.stream()
                .map(provider -> provider.getId())
                .collect(java.util.stream.Collectors.toSet());

        Set<UUID> invalidIds = new HashSet<>(requestedIds);
        invalidIds.removeAll(eligibleIds);
        if (!invalidIds.isEmpty()) {
            throw new InvalidStreamingPreferenceException(invalidIds);
        }

        var currentPreferences = preferenceRepository.findAllWithProviderByUserId(userId);
        var preferencesToDelete = currentPreferences.stream()
                .filter(preference -> !requestedIds.contains(preference.getProvider().getId()))
                .toList();
        preferenceRepository.deleteAll(preferencesToDelete);

        Set<UUID> currentIds = currentPreferences.stream()
                .map(preference -> preference.getProvider().getId())
                .collect(java.util.stream.Collectors.toSet());
        var preferencesToCreate = eligibleProviders.stream()
                .filter(provider -> requestedIds.contains(provider.getId()))
                .filter(provider -> !currentIds.contains(provider.getId()))
                .map(provider -> new UserStreamingPreferenceEntity(user, provider))
                .toList();
        preferenceRepository.saveAll(preferencesToCreate);

        return new StreamingPreferencesResponse(
                eligibleProviders.stream()
                        .map(provider -> provider.getId())
                        .filter(requestedIds::contains)
                        .toList()
        );
    }

    private StreamingPreferencesResponse response(UUID userId) {
        return new StreamingPreferencesResponse(
                preferenceRepository.findAllWithProviderByUserId(userId).stream()
                        .map(preference -> preference.getProvider().getId())
                        .toList()
        );
    }
}
