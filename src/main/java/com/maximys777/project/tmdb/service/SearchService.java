package com.maximys777.project.tmdb.service;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.common.MediaType;
import com.maximys777.project.tmdb.dto.response.search.GenreListResponse;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchDetailsResponse;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchResponse;
import com.maximys777.project.tmdb.dto.response.search.SearchDropdownResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final TMDBService tmdbService;

    private final Map<String, Map<Integer, String>> genreCache = new ConcurrentHashMap<>();

    public Mono<List<SearchDropdownResponse>> search(String query, LanguageType language) {
        return tmdbService.multiSearch(query, language, 1)
                .map(tmdbResponse -> tmdbResponse.results().stream()
                        .filter(item -> MediaType.movie.equals(item.mediaType()) ||
                                MediaType.tv.equals(item.mediaType()))
                        .limit(5)
                        .map(item -> {
                            List<String> genreNames = item.genreIds().stream()
                                    .map(id -> getGenreName(Math.toIntExact(id), language))
                                    .toList();

                            String title = MediaType.movie.equals(item.mediaType()) ? item.title() : item.name();
                            String date = MediaType.movie.equals(item.mediaType()) ? item.releaseDate() : item.firstAirDate();
                            String year = (date != null && date.length() >= 4) ? date.substring(0, 4) : "";

                            return SearchDropdownResponse.builder()
                                    .id(Long.valueOf(item.id()))
                                    .mediaType(item.mediaType().name())
                                    .title(title)
                                    .posterUrl(item.posterPath())
                                    .releaseYear(year)
                                    .genres(genreNames)
                                    .build();
                        })
                        .toList());
    }

    @Scheduled(fixedRate = 86400000)
    @Cacheable(value = "genres")
    public void refreshGenreCache() {
        log.info("Refreshing genre cache is start");

        for (LanguageType language : LanguageType.values()) {
            Map<Integer, String> combinedGenres = new ConcurrentHashMap<>();

            try {
                GenreListResponse movieGenres = tmdbService.getMovieGenres(language).block();

                if (movieGenres != null && movieGenres.genres() != null) {
                    movieGenres.genres().forEach(genre -> combinedGenres.put(genre.id(), genre.name()));
                }

                GenreListResponse tvGenres = tmdbService.getTvGenres(language).block();

                if (tvGenres != null && tvGenres.genres() != null) {
                    tvGenres.genres().forEach(genre -> combinedGenres.put(genre.id(), genre.name()));
                }

                genreCache.put(language.name(), combinedGenres);
            } catch (Exception e) {
                log.error("Error refreshing genre cache for language {}, {}", language.name(), e.getMessage());
            }
        }
        log.info("Refreshing genre cache is complete");
    }

    public Mono<MultiSearchResponse> fullSearch(String query, LanguageType language, int page) {
        return tmdbService.multiSearch(query, language, page)
                .map(response -> {
                    List<MultiSearchDetailsResponse> results = response.results().stream()
                            .map(item -> {
                                List<String> genresName = item.genreIds() != null ? item.genreIds().stream()
                                        .map(id -> getGenreName(Math.toIntExact(id), language))
                                        .toList() : List.of();

                                return MultiSearchDetailsResponse.builder()
                                        .id(item.id())
                                        .title(item.title())
                                        .overview(item.overview())
                                        .posterPath(item.posterPath())
                                        .mediaType(item.mediaType())
                                        .genreIds(item.genreIds())
                                        .genreNames(genresName)
                                        .releaseDate(item.releaseDate())
                                        .firstAirDate(item.firstAirDate())
                                        .voteAverage(item.voteAverage())
                                        .name(item.name())
                                        .build();
                            }).toList();
                    
                    return new MultiSearchResponse(response.page(), results, response.totalPages(), response.totalResults());
                });
    }

    public String getGenreName(Integer genreId, LanguageType language) {
        Map<Integer, String> langMap = genreCache.getOrDefault(language.name(), genreCache.get("en"));
        return langMap != null ? langMap.getOrDefault(genreId, "Unknown") : "Unknown";
    }
}
