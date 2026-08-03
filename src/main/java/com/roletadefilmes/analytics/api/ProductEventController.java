package com.roletadefilmes.analytics.api;

import com.roletadefilmes.analytics.api.dto.ProductEventRequest;
import com.roletadefilmes.analytics.service.ProductEventService;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/events")
public class ProductEventController {

    private final ProductEventService eventService;

    public ProductEventController(ProductEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Void> record(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProductEventRequest request
    ) {
        eventService.record(user.userId(), request);
        return ResponseEntity.accepted().build();
    }
}
