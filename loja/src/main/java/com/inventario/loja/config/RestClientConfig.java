package com.inventario.loja.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient centralRestClient(
            @Value("${central.base-url}") String baseUrl,
            @Value("${central.http.connect-timeout-ms:300}") long connectTimeoutMs,
            @Value("${central.http.read-timeout-ms:500}") long readTimeoutMs
    ) {
        var factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}