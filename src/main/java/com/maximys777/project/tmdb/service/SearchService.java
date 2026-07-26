package com.maximys777.project.tmdb.service;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.common.MediaType;
import com.maximys777.project.tmdb.dto.response.search.GenreListResponse;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchDetailsResponse;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchResponse;
import com.maximys777.project.tmdb.dto.response.search.SearchDropdownResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final TMDBService tmdbService;

    private final Map<LanguageType, Map<Integer, String>> genreCache = new ConcurrentHashMap<>();

    public Mono<List<SearchDropdownResponse>> search(String query, LanguageType language) {
        return tmdbService.multiSearch(query, language, 1)
                .map(tmdbResponse -> tmdbResponse.results().stream()
                        .filter(item -> MediaType.movie.equals(item.mediaType()) ||
                                MediaType.tv.equals(item.mediaType()))
                        .sorted(Comparator.comparingDouble((MultiSearchDetailsResponse moviePopularity) ->
                                        moviePopularity.popularity() == null ? 0.0 : moviePopularity.popularity())
                                .reversed())
                        .limit(5)
                        .map(item -> {
                            List<String> extractGenreNames = getGenreName(item.genreIds(), language);

                            String title = MediaType.movie.equals(item.mediaType()) ? item.title() : item.name();
                            String date = MediaType.movie.equals(item.mediaType()) ? item.releaseDate() : item.firstAirDate();
                            String year = (date != null && date.length() >= 4) ? date.substring(0, 4) : "";

                            return SearchDropdownResponse.builder()
                                    .id(Long.valueOf(item.id()))
                                    .mediaType(item.mediaType().name())
                                    .title(title)
                                    .posterUrl(item.posterPath())
                                    .releaseYear(year)
                                    .genres(extractGenreNames)
                                    .build();
                        })
                        .toList());
    }

    @Scheduled(fixedRate = 86400000)
    public void refreshGenreCache() {
        log.info("Refreshing genre cache is start");

        List<Mono<Void>> monos = Arrays.stream(LanguageType.values())
                .map(language ->
                        Mono.zip(
                                        tmdbService.getMovieGenres(language),
                                        tmdbService.getTvGenres(language)
                                )
                                .doOnNext(tuple -> {
                                    Map<Integer, String> combinedGenres = new ConcurrentHashMap<>();

                                    GenreListResponse movieGenres = tuple.getT1();
                                    if (movieGenres.genres() != null) {
                                        movieGenres.genres().forEach(genre ->
                                                combinedGenres.put(genre.id(), genre.name()));
                                    }

                                    GenreListResponse tvGenres = tuple.getT2();
                                    if (tvGenres.genres() != null) {
                                        tvGenres.genres().forEach(genre ->
                                                combinedGenres.put(genre.id(), genre.name()));
                                    }

                                    genreCache.put(language, combinedGenres);
                                })
                                .onErrorResume(e -> {
                                    log.error("Error refreshing genre cache for language {}, {}", language.name(), e.getMessage());
                                    return Mono.empty();
                                })
                                .then()
                )
                .toList();

        Mono.when(monos.toArray(new Mono[0]))
                .doOnSuccess(v -> log.info("Refreshing genre cache is complete"))
                .doOnError(e -> log.error("Error refreshing genre cache", e))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    public Mono<MultiSearchResponse> fullSearch(String query, LanguageType language, int page) {
        return tmdbService.multiSearch(query, language, page)
                .map(response -> {
                    List<MultiSearchDetailsResponse> results = response.results().stream()
                            .sorted(Comparator.comparingDouble((MultiSearchDetailsResponse moviePopularity) ->
                                            moviePopularity.popularity() == null ? 0.0 : moviePopularity.popularity())
                                    .reversed())
                            .map(item -> {
                                List<String> extractGenreNames = getGenreName(item.genreIds(), language);

                                return MultiSearchDetailsResponse.builder()
                                        .id(item.id())
                                        .title(item.title())
                                        .overview(item.overview())
                                        .posterPath(item.posterPath())
                                        .mediaType(item.mediaType())
                                        .genreIds(item.genreIds())
                                        .genreNames(extractGenreNames)
                                        .releaseDate(item.releaseDate())
                                        .firstAirDate(item.firstAirDate())
                                        .voteAverage(item.voteAverage())
                                        .name(item.name())
                                        .build();
                            }).toList();

                    return new MultiSearchResponse(response.page(), results, response.totalPages(), response.totalResults());
                });
    }

    private List<String> getGenreName(List<Long> genreId, LanguageType language) {
        if (genreId == null || genreId.isEmpty()) {
            return List.of();
        }

        return genreId.stream()
                .map(id -> Optional.ofNullable(genreCache.get(language))
                        .or(() -> Optional.ofNullable(genreCache.get(LanguageType.en)))
                        .map(langMap -> langMap.get(Math.toIntExact(id)))
                        .orElse("Unknown"))
                .toList();
    }
}
