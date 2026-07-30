package com.roletadefilmes.catalog.api;

import com.roletadefilmes.catalog.api.dto.ProviderCatalogResponse;
import com.roletadefilmes.catalog.api.dto.VibeCatalogResponse;
import com.roletadefilmes.catalog.service.CatalogService;
import com.roletadefilmes.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderCatalogResponse>> listProviders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(catalogService.listProviders());
    }

    @GetMapping("/vibes")
    public ResponseEntity<List<VibeCatalogResponse>> listVibes(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(catalogService.listVibes());
    }
}
