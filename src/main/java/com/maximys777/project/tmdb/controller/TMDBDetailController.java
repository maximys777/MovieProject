package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.dto.response.movie.MovieDetails;
import com.maximys777.project.tmdb.service.TMDBService;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.TvShowDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/details")
public class TMDBDetailController {
    private final TMDBService tmdbService;

    @GetMapping("/tv-shows/{id}")
    public Mono<TvShowDetailsResponse> getTvShowDetails(@PathVariable Long id,
                                                        @RequestParam(required = false, defaultValue = "en") LanguageType language) {
        return tmdbService.getTvShowDetails(id, language);
    }

    @GetMapping("/movies/{movieId}")
    public Mono<MovieDetails> getMovieDetails(@PathVariable Long movieId,
                                              @RequestParam(required = false, defaultValue = "en") LanguageType language) {
        return tmdbService.getMovieDetails(movieId, language);
    }
}
