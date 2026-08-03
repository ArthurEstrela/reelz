package com.roletadefilmes.feedback.api;

import com.roletadefilmes.feedback.api.dto.BetaFeedbackRequest;
import com.roletadefilmes.feedback.service.BetaFeedbackService;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
public class BetaFeedbackController {

    private final BetaFeedbackService feedbackService;

    public BetaFeedbackController(BetaFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<Void> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BetaFeedbackRequest request
    ) {
        feedbackService.submit(user.userId(), request);
        return ResponseEntity.accepted().build();
    }
}
