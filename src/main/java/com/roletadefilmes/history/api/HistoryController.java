package com.roletadefilmes.history.api;

import com.roletadefilmes.history.api.dto.HistoryResponse;
import com.roletadefilmes.history.api.dto.SaveHistoryRequest;
import com.roletadefilmes.history.service.HistoryService;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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
}
