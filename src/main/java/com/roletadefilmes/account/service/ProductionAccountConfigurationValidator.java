package com.roletadefilmes.account.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionAccountConfigurationValidator {

    public ProductionAccountConfigurationValidator(
            @Value("${reelz.account.mail-mode}") String mailMode,
            @Value("${reelz.account.public-url}") String publicUrl
    ) {
        if (!"SMTP".equalsIgnoreCase(mailMode)) {
            throw new IllegalStateException("Producao exige ACCOUNT_MAIL_MODE=SMTP");
        }
        if (!publicUrl.startsWith("https://")) {
            throw new IllegalStateException("Producao exige PUBLIC_APP_URL com HTTPS");
        }
    }
}
