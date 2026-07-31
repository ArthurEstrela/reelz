package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.api.dto.ProviderCatalogResponse;
import com.roletadefilmes.catalog.api.dto.VibeCatalogResponse;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import com.roletadefilmes.vibe.persistence.repository.VibeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogService {

    private final StreamingProviderRepository providerRepository;
    private final VibeRepository vibeRepository;
    private final UserAccountRepository userRepository;

    public CatalogService(
            StreamingProviderRepository providerRepository,
            VibeRepository vibeRepository,
            UserAccountRepository userRepository
    ) {
        this.providerRepository = providerRepository;
        this.vibeRepository = vibeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderCatalogResponse> listProviders(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return providerRepository.findEligibleForCountry(user.getCountryCode()).stream()
                .map(provider -> new ProviderCatalogResponse(provider.getId(), provider.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VibeCatalogResponse> listVibes() {
        return vibeRepository.findAllByActiveTrueOrderByLabelAsc().stream()
                .map(vibe -> new VibeCatalogResponse(vibe.getId(), vibe.getLabel()))
                .toList();
    }
}
