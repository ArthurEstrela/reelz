package com.roletadefilmes.history.api;

import com.roletadefilmes.history.api.dto.HistoryResponse;
import com.roletadefilmes.history.api.dto.SaveHistoryRequest;
import com.roletadefilmes.history.api.dto.UserMovieHistoryResponse;
import com.roletadefilmes.history.domain.UserMovieStatus;
import com.roletadefilmes.history.service.HistoryService;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping
    public ResponseEntity<HistoryResponse> save(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody SaveHistoryRequest request
    ) {
        return ResponseEntity.ok(historyService.save(authenticatedUser.userId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<UserMovieHistoryResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "WATCHED") UserMovieStatus status,
            @PageableDefault(size = 24) Pageable pageable
    ) {
        return ResponseEntity.ok(historyService.list(authenticatedUser.userId(), status, pageable));
    }

    @DeleteMapping("/watchlist/{movieId}")
    public ResponseEntity<Void> removeFromWatchlist(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long movieId
    ) {
        historyService.removeFromWatchlist(authenticatedUser.userId(), movieId);
        return ResponseEntity.noContent().build();
    }
}
