package com.maximys777.project.tmdb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TMDBConfiguration {

    @Value("${spring.tmdb.access.token}")
    private String accessToken;

    @Bean
    public WebClient tmdbWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.themoviedb.org/3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }
}
