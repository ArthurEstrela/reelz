package com.roletadefilmes.billing.integration;

import com.roletadefilmes.billing.domain.BillingPlanCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("reelz.billing.abacatepay")
public record AbacatePayProperties(
        boolean enabled,
        String apiKey,
        String webhookSecret,
        String webhookHmacKey,
        String monthlyProductId,
        String annualProductId,
        int monthlyPriceCents,
        int annualPriceCents,
        String publicAppUrl,
        List<String> methods,
        boolean acceptDevEvents,
        Duration connectTimeout,
        Duration readTimeout
) {
    public String productId(BillingPlanCode planCode) {
        return switch (planCode) {
            case PREMIUM_MONTHLY -> monthlyProductId;
            case PREMIUM_ANNUAL -> annualProductId;
        };
    }

    public int priceCents(BillingPlanCode planCode) {
        return switch (planCode) {
            case PREMIUM_MONTHLY -> monthlyPriceCents;
            case PREMIUM_ANNUAL -> annualPriceCents;
        };
    }

    public boolean isPlanAvailable(BillingPlanCode planCode) {
        return enabled && StringUtils.hasText(apiKey) && StringUtils.hasText(productId(planCode));
    }

    public List<String> safeMethods() {
        return methods == null || methods.isEmpty() ? List.of("CARD") : List.copyOf(methods);
    }

    public String appUrl(String path) {
        var base = publicAppUrl.endsWith("/") ? publicAppUrl.substring(0, publicAppUrl.length() - 1) : publicAppUrl;
        return base + path;
    }
}
