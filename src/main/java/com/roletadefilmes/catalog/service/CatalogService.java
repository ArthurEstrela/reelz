package com.roletadefilmes.catalog.service;

import com.roletadefilmes.catalog.api.dto.ProviderCatalogResponse;
import com.roletadefilmes.catalog.api.dto.VibeCatalogResponse;
import com.roletadefilmes.streaming.persistence.repository.StreamingProviderRepository;
import com.roletadefilmes.vibe.persistence.repository.VibeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final StreamingProviderRepository providerRepository;
    private final VibeRepository vibeRepository;

    public CatalogService(
            StreamingProviderRepository providerRepository,
            VibeRepository vibeRepository
    ) {
        this.providerRepository = providerRepository;
        this.vibeRepository = vibeRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderCatalogResponse> listProviders() {
        return providerRepository.findAllByActiveTrueOrderByDisplayPriorityAsc().stream()
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
