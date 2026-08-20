package com.roletadefilmes.billing.api;

import com.roletadefilmes.billing.api.dto.WebhookResponse;
import com.roletadefilmes.billing.service.BillingWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BillingWebhookController {

    private final BillingWebhookService webhookService;

    public BillingWebhookController(BillingWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/api/v1/webhooks/abacatepay")
    public ResponseEntity<WebhookResponse> abacatePay(
            @RequestParam String webhookSecret,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String webhookSignature,
            @RequestHeader(value = "X-Abacate-Signature", required = false) String abacateSignature,
            @RequestBody byte[] rawBody
    ) {
        var signature = webhookSignature != null ? webhookSignature : abacateSignature;
        return ResponseEntity.ok(webhookService.handle(rawBody, webhookSecret, signature));
    }
}
