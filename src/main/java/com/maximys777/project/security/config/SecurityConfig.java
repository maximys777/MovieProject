package com.maximys777.project.security.config;

import com.maximys777.project.security.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOAuth2UserService))
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/grantcode"))
                        .defaultSuccessUrl("/profile.html", true))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/watchlist-movies/*/user").permitAll()
                        .requestMatchers(HttpMethod.POST, "/watchlist-movies").authenticated()
                        .requestMatchers(HttpMethod.GET, "/watchlist-tv-shows/*/user").permitAll()
                        .requestMatchers("/index.html", "/js/**", "/css/**", "/login.html", "/details/**", "/trending/**", "/search/**", "/search.html").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
