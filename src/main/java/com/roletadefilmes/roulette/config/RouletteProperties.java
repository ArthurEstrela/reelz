package com.roletadefilmes.roulette.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reelz.roulette")
public record RouletteProperties(RouletteCatalogSource catalogSource) {

    public RouletteProperties {
        catalogSource = catalogSource == null ? RouletteCatalogSource.ALL : catalogSource;
    }
}
