package com.example.nextjsbackendadmin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Spring configuration for shared clients and property binding.
 */
@Configuration
@EnableConfigurationProperties({AppProperties.class, AdminAuthProperties.class})
public class AppConfig {

    @Bean
    public WebClient webClient() {
        // Increase in-memory buffer for JSON payloads and error bodies.
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .exchangeStrategies(strategies)
                .build();
    }
}
