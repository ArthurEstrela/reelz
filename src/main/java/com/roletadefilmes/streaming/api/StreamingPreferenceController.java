package com.roletadefilmes.streaming.api;

import com.roletadefilmes.security.AuthenticatedUser;
import com.roletadefilmes.streaming.api.dto.StreamingPreferencesResponse;
import com.roletadefilmes.streaming.api.dto.UpdateStreamingPreferencesRequest;
import com.roletadefilmes.streaming.service.StreamingPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/streaming-preferences")
public class StreamingPreferenceController {

    private final StreamingPreferenceService preferenceService;

    public StreamingPreferenceController(StreamingPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<StreamingPreferencesResponse> get(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(preferenceService.get(authenticatedUser.userId()));
    }

    @PutMapping
    public ResponseEntity<StreamingPreferencesResponse> replace(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateStreamingPreferencesRequest request
    ) {
        return ResponseEntity.ok(preferenceService.replace(
                authenticatedUser.userId(),
                request.providerIds()
        ));
    }
}
