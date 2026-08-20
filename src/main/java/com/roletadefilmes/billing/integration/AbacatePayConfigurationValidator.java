package com.roletadefilmes.billing.integration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Set;

@Component
public class AbacatePayConfigurationValidator {

    private static final Set<String> SUPPORTED_METHODS = Set.of("CARD", "PIX");
    private final AbacatePayProperties properties;

    public AbacatePayConfigurationValidator(AbacatePayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (!properties.enabled()) return;
        require(properties.apiKey(), "ABACATEPAY_API_KEY");
        require(properties.webhookSecret(), "ABACATEPAY_WEBHOOK_SECRET");
        require(properties.webhookHmacKey(), "ABACATEPAY_WEBHOOK_HMAC_KEY");
        require(properties.monthlyProductId(), "ABACATEPAY_MONTHLY_PRODUCT_ID");
        require(properties.annualProductId(), "ABACATEPAY_ANNUAL_PRODUCT_ID");
        require(properties.publicAppUrl(), "PUBLIC_APP_URL");
        if (properties.monthlyPriceCents() <= 0 || properties.annualPriceCents() <= 0) {
            throw new IllegalStateException("Os preços Premium precisam ser maiores que zero.");
        }
        if (properties.safeMethods().stream().anyMatch(method -> !SUPPORTED_METHODS.contains(method))) {
            throw new IllegalStateException("ABACATEPAY_METHODS aceita apenas CARD e PIX.");
        }
        var appUri = URI.create(properties.publicAppUrl());
        if (appUri.getHost() == null || !("http".equals(appUri.getScheme()) || "https".equals(appUri.getScheme()))) {
            throw new IllegalStateException("PUBLIC_APP_URL precisa ser uma URL HTTP(S) absoluta.");
        }
    }

    private void require(String value, String variable) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(variable + " é obrigatória quando ABACATEPAY_ENABLED=true.");
        }
    }
}
