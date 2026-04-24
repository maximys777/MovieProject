package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.common.TimeWindow;
import com.maximys777.project.tmdb.dto.response.movie.TrendMovieResponse;
import com.maximys777.project.tmdb.dto.response.tvshow.TrendTvShowResponse;
import com.maximys777.project.tmdb.service.TMDBService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trending")
public class TMDBController {
    private final TMDBService tmdbService;

    @GetMapping("/movies/{timeWindow}")
    public Flux<TrendMovieResponse> getTrendingMovies(@PathVariable TimeWindow timeWindow,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam LanguageType language) {
        return tmdbService.getTrendingMovies(timeWindow, page, language);
    }

    @GetMapping("/tv-shows/{timeWindow}")
    public Flux<TrendTvShowResponse> getTrendingTvShows(@PathVariable TimeWindow timeWindow,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam LanguageType language) {
        return tmdbService.getTrendingTvShows(timeWindow, page, language);
    }
}
