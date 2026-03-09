package com.maximys777.project.tmdb.service;

import com.maximys777.project.tmdb.common.TimeWindow;
import com.maximys777.project.tmdb.dto.response.movie.TrendMovieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
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

    public Flux<TrendMovieResponse> getTrendingMovies(TimeWindow timeWindow, int page) {
        return tmdbWebClient.get()
                .uri("/{timeWindow}?page={page}", timeWindow, page)
                .retrieve()
                .bodyToFlux(TrendMovieResponse.class);

    }

    @Scheduled(fixedRate = 86400000)
    public void getTrendingMoviesForOneDay() {
        tmdbWebClient.get()
                .uri("/day?page=1")
                .retrieve()
                .bodyToFlux(TrendMovieResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                System.out.println("Retry request. Attempt #" + (retrySignal.totalRetries() + 1))
                        )
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Service unavailable after" + retryBackoffSpec.maxAttempts + " attempts", retrySignal.failure())))
                .subscribe(
                        response -> System.out.println("Movies: " + response),
                        error -> System.out.println("Error: " + error.getMessage())
                );
    }

    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            HttpStatus status = (HttpStatus) ((WebClientResponseException) throwable).getStatusCode();
            return status == HttpStatus.SERVICE_UNAVAILABLE || status == HttpStatus.TOO_MANY_REQUESTS;
        }

        return throwable instanceof WebClientResponseException;
    }
}
