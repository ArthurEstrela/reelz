package com.roletadefilmes.billing.api;

import com.roletadefilmes.billing.api.dto.BillingPlanResponse;
import com.roletadefilmes.billing.api.dto.CheckoutResponse;
import com.roletadefilmes.billing.api.dto.CreateCheckoutRequest;
import com.roletadefilmes.billing.api.dto.SubscriptionResponse;
import com.roletadefilmes.billing.service.BillingService;
import com.roletadefilmes.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<BillingPlanResponse>> plans() {
        return ResponseEntity.ok(billingService.plans());
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> current(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(billingService.current(principal.userId()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateCheckoutRequest request
    ) {
        return ResponseEntity.ok(billingService.createCheckout(principal.userId(), request.planCode()));
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(billingService.cancel(principal.userId()));
    }
}
