package com.maximys777.project.security.config;

import com.maximys777.project.security.handler.CustomAuthenticationFailureHandler;
import com.maximys777.project.security.handler.CustomAuthenticationSuccessHandler;
import com.maximys777.project.security.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RequestCache requestCache) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint()))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOAuth2UserService))
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/grantcode"))
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/watchlist-movies/*/user").permitAll()
                        .requestMatchers(HttpMethod.GET, "/watchlist-tv-shows/*/user").permitAll()
                        .requestMatchers("/index.html", "/js/**", "/css/**", "/login.html", "/details/**", "/trending/**", "/search/**", "/search.html").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .requestCache(cache -> cache.requestCache(requestCache))
                .build();
    }

    private static final RequestMatcher API_REQUESTS = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern("/watchlist-movies/**"),
            PathPatternRequestMatcher.pathPattern("/watchlist-tv-shows/**"),
            new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

    @Bean
    public RequestCache navigationRequestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

        requestCache.setRequestMatcher(new AndRequestMatcher(
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/**"),
                new NegatedRequestMatcher(API_REQUESTS),
                new RequestHeaderRequestMatcher("Sec-Fetch-Mode", "navigate")
        ));
        return requestCache;
    }

    private AuthenticationEntryPoint entryPoint() {
        return DelegatingAuthenticationEntryPoint.builder()
                .defaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login.html"))
                .addEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API_REQUESTS)
                .build();
    }
}
