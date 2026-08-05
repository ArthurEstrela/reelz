package com.roletadefilmes.social.api;

import com.roletadefilmes.roulette.api.dto.RouletteSpinRequest;
import com.roletadefilmes.security.AuthenticatedUser;
import com.roletadefilmes.social.api.dto.CreateSocialRoomRequest;
import com.roletadefilmes.social.api.dto.JoinSocialRoomRequest;
import com.roletadefilmes.social.api.dto.SocialRoomResponse;
import com.roletadefilmes.social.api.dto.SocialRoomSummaryResponse;
import com.roletadefilmes.social.api.dto.SocialSpinResponse;
import com.roletadefilmes.social.api.dto.UpdateSocialPreferenceRequest;
import com.roletadefilmes.social.service.SocialRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/rooms")
public class SocialRoomController {

    private final SocialRoomService service;

    public SocialRoomController(SocialRoomService service) {
        this.service = service;
    }

    @GetMapping
    public List<SocialRoomSummaryResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return service.list(principal.userId());
    }

    @PostMapping
    public ResponseEntity<SocialRoomResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateSocialRoomRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(principal.userId(), request.type()));
    }

    @PostMapping("/join")
    public SocialRoomResponse join(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody JoinSocialRoomRequest request
    ) {
        return service.join(principal.userId(), request.inviteCode());
    }

    @GetMapping("/{roomId}")
    public SocialRoomResponse get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID roomId
    ) {
        return service.get(principal.userId(), roomId);
    }

    @PostMapping("/{roomId}/spin")
    public SocialSpinResponse spin(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID roomId,
            @Valid @RequestBody RouletteSpinRequest request
    ) {
        return service.spin(principal.userId(), roomId, request);
    }

    @PutMapping("/{roomId}/members/me/preferences")
    public SocialRoomResponse updatePreference(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateSocialPreferenceRequest request
    ) {
        return service.updatePreference(principal.userId(), roomId, request);
    }

    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID roomId
    ) {
        service.leave(principal.userId(), roomId);
        return ResponseEntity.noContent().build();
    }
}
