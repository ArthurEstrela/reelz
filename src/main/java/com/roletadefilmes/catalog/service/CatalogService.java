package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.api.dto.ProviderCatalogResponse;
import com.roletadefilmes.catalog.api.dto.VibeCatalogResponse;
import com.roletadefilmes.roulette.config.RouletteProperties;
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
    private final RouletteProperties rouletteProperties;

    public CatalogService(
            StreamingProviderRepository providerRepository,
            VibeRepository vibeRepository,
            UserAccountRepository userRepository,
            RouletteProperties rouletteProperties
    ) {
        this.providerRepository = providerRepository;
        this.vibeRepository = vibeRepository;
        this.userRepository = userRepository;
        this.rouletteProperties = rouletteProperties;
    }

    @Transactional(readOnly = true)
    public List<ProviderCatalogResponse> listProviders(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return providerRepository.findEligibleForCountryAndCatalogSource(
                        user.getCountryCode(),
                        rouletteProperties.catalogSource().name()
                ).stream()
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
