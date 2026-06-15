package com.maximys777.project.tmdb.service;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.common.TimeWindow;
import com.maximys777.project.tmdb.dto.response.movie.MovieDetails;
import com.maximys777.project.tmdb.dto.response.movie.TrendMovieResponse;
import com.maximys777.project.tmdb.dto.response.movie.TrendingMovieResultResponse;
import com.maximys777.project.tmdb.dto.response.search.GenreListResponse;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchDetailsResponse;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchResponse;
import com.maximys777.project.tmdb.dto.response.tvshow.TrendTvShowResponse;
import com.maximys777.project.tmdb.dto.response.tvshow.TvShowResultResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.SeasonResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.TvShowDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TMDBService {
    private final WebClient tmdbWebClient;

    @Cacheable(value = "trendMovies", key = "{#timeWindow, #page, #language}")
    public Flux<TrendMovieResponse> getTrendingMovies(TimeWindow timeWindow, int page, LanguageType language) {
        return tmdbWebClient.get()
                .uri("/trending/movie/{timeWindow}?page={page}&language={language}", timeWindow, page, language)
                .retrieve()
                .bodyToFlux(TrendMovieResponse.class)
                .map(response -> {
                    List<TrendingMovieResultResponse> filterResponse = response.results().stream()
                            .filter(item -> item.voteAverage() != null && item.voteAverage() > 0.0)
                            .toList();

                    return new TrendMovieResponse(response.page(), filterResponse, response.totalPages());
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                System.out.println("Retry request. Attempt #" + (retrySignal.totalRetries() + 1))
                        )
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Service unavailable after" + retryBackoffSpec.maxAttempts + " attempts", retrySignal.failure())));
    }

    @Cacheable(value = "trendTvShows", key = "{#timeWindow, #page, #language}")
    public Flux<TrendTvShowResponse> getTrendingTvShows(TimeWindow timeWindow, int page, LanguageType language) {
        return tmdbWebClient.get()
                .uri("/trending/tv/{timeWindow}?page={page}&language={language}", timeWindow, page, language)
                .retrieve()
                .bodyToFlux(TrendTvShowResponse.class)
                .map(response -> {
                    List<TvShowResultResponse> filterResponse = response.results().stream()
                            .filter(item -> item.voteAverage() != null && item.voteAverage().doubleValue() > 0.0)
                            .toList();

                    return new TrendTvShowResponse(response.page(), filterResponse, response.totalPages());
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                System.out.println("Retry request. Attempt #" + (retrySignal.totalRetries() + 1))
                        )
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Service unavailable after" + retryBackoffSpec.maxAttempts + " attempts", retrySignal.failure())));
    }

    @Cacheable(value = "tvShowDetails", key = "{#tvShowId, #language}")
    public Mono<TvShowDetailsResponse> getTvShowDetails(Long tvShowId, LanguageType language) {
        return tmdbWebClient.get()
                .uri("/tv/{tvShowId}?language={language}", tvShowId, language)
                .retrieve()
                .bodyToMono(TvShowDetailsResponse.class)
                .map(response -> {
                    List<SeasonResponse> filterSeason = response.seasons().stream()
                            .filter(s -> s.seasonNumber() > 0)
                            .toList();

                    return new TvShowDetailsResponse(
                            response.id(),
                            response.name(),
                            response.posterPath(),
                            response.overview(),
                            response.voteAverage(),
                            response.firstAirDate(),
                            response.numberOfEpisodes(),
                            response.numberOfSeasons(),
                            response.genres(),
                            response.productionCountries(),
                            filterSeason
                    );
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError));
    }

    @Cacheable(value = "tvShowDetails", key = "#tvShowId")
    public Mono<TvShowDetailsResponse> getTvShowDetails(Long tvShowId) {
        return getTvShowDetails(tvShowId, LanguageType.en);
    }

    @Cacheable(value = "movieDetails", key = "{#movieId, #language}")
    public Mono<MovieDetails> getMovieDetails(Long movieId, LanguageType language) {
        return tmdbWebClient.get()
                .uri("/movie/{movieId}?language={language}", movieId, language)
                .retrieve()
                .bodyToMono(MovieDetails.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError));
    }

    @Cacheable(value = "movieGenres", key = "#language")
    public Mono<GenreListResponse> getMovieGenres(LanguageType language) {
        return tmdbWebClient.get()
                .uri("/genre/movie/list?language={language}", language)
                .retrieve()
                .bodyToMono(GenreListResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError));
    }

    @Cacheable(value = "tvShowGenres", key = "#language")
    public Mono<GenreListResponse> getTvGenres(LanguageType language) {
        return tmdbWebClient.get()
                .uri("/genre/tv/list?language={language}", language)
                .retrieve()
                .bodyToMono(GenreListResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError));
    }

    public Mono<MultiSearchResponse> multiSearch(String query, LanguageType language, int page) {
        return tmdbWebClient.get()
                .uri("/search/multi?query={query}&language={language}&page={page}", query, language, page)
                .retrieve()
                .bodyToMono(MultiSearchResponse.class)
                .map(response -> {
                    List<MultiSearchDetailsResponse> filteredResponse = response.results().stream()
                            .filter(item -> item.voteAverage() != null && item.voteAverage() > 0.0 ||
                                    item.posterPath() != null && !item.posterPath().isEmpty())
                            .toList();

                    return new MultiSearchResponse(page, filteredResponse, response.totalPages(), response.totalResults());
                });
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
