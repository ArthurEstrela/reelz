package com.roletadefilmes.billing.integration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
public class MercadoPagoConfigurationValidator {

    private final MercadoPagoProperties properties;

    public MercadoPagoConfigurationValidator(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (!properties.enabled()) return;
        require(properties.accessToken(), "MERCADOPAGO_ACCESS_TOKEN");
        require(properties.webhookSecret(), "MERCADOPAGO_WEBHOOK_SECRET");
        require(properties.publicAppUrl(), "PUBLIC_APP_URL");
        if (properties.monthlyPriceCents() <= 0 || properties.annualPriceCents() <= 0) {
            throw new IllegalStateException("Os preços Premium precisam ser maiores que zero.");
        }
        var appUri = URI.create(properties.publicAppUrl());
        if (appUri.getHost() == null || !"https".equals(appUri.getScheme())) {
            throw new IllegalStateException("PUBLIC_APP_URL precisa ser uma URL HTTPS absoluta para pagamentos.");
        }
    }

    private void require(String value, String variable) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(variable + " é obrigatória quando MERCADOPAGO_ENABLED=true.");
        }
    }
}
