package com.maximys777.project.tmdb.service;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.common.TimeWindow;
import com.maximys777.project.tmdb.dto.response.movie.TrendMovieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TMDBService {
    private final WebClient tmdbWebClient;

    @Cacheable(value = "movies", key = "{#timeWindow, #page, #language}")
    public Flux<TrendMovieResponse> getTrendingMovies(TimeWindow timeWindow, int page, LanguageType language) {
        return tmdbWebClient.get()
                .uri("/{timeWindow}?page={page}&language={language}", timeWindow, page, language)
                .retrieve()
                .bodyToFlux(TrendMovieResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                System.out.println("Retry request. Attempt #" + (retrySignal.totalRetries() + 1))
                        )
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Service unavailable after" + retryBackoffSpec.maxAttempts + " attempts", retrySignal.failure())));
    }

    private boolean isRetryableError(Throwable throwable) {
        boolean exception = throwable instanceof WebClientResponseException;

        if (exception) {
            HttpStatus status = (HttpStatus) ((WebClientResponseException) throwable).getStatusCode();
            return status == HttpStatus.SERVICE_UNAVAILABLE || status == HttpStatus.TOO_MANY_REQUESTS;
        }

        return exception;
    }
}
