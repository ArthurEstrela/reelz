package com.roletadefilmes.billing.integration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AbacatePayProperties.class)
public class AbacatePayConfiguration {

    @Bean
    RestClient abacatePayRestClient(RestClient.Builder builder, AbacatePayProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl("https://api.abacatepay.com/v2")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }
}
