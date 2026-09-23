package com.roletadefilmes.billing.integration;

import com.roletadefilmes.billing.domain.BillingPlanCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@ConfigurationProperties("reelz.billing.mercadopago")
public record MercadoPagoProperties(
        boolean enabled,
        String accessToken,
        String webhookSecret,
        int monthlyPriceCents,
        int annualPriceCents,
        String publicAppUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
    public int priceCents(BillingPlanCode planCode) {
        return switch (planCode) {
            case PREMIUM_MONTHLY -> monthlyPriceCents;
            case PREMIUM_ANNUAL -> annualPriceCents;
        };
    }

    public boolean isPlanAvailable(BillingPlanCode planCode) {
        return enabled && StringUtils.hasText(accessToken) && priceCents(planCode) > 0;
    }

    public String appUrl(String path) {
        var base = publicAppUrl.endsWith("/")
                ? publicAppUrl.substring(0, publicAppUrl.length() - 1)
                : publicAppUrl;
        return base + path;
    }
}
