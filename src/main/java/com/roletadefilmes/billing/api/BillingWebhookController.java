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

    @PostMapping("/api/v1/webhooks/mercadopago")
    public ResponseEntity<WebhookResponse> mercadoPago(
            @RequestParam(name = "data.id", required = false) String dottedDataId,
            @RequestParam(name = "data_id", required = false) String underscoredDataId,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestBody byte[] rawBody
    ) {
        var dataId = dottedDataId != null ? dottedDataId : underscoredDataId;
        return ResponseEntity.ok(webhookService.handle(rawBody, dataId, signature, requestId));
    }
}
