package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.TimeWindow;
import com.maximys777.project.tmdb.dto.response.movie.TrendMovieResponse;
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
@RequestMapping("/movies")
public class TMDBController {
    private final TMDBService tmdbService;

    @GetMapping("/{timeWindow}")
    public Flux<TrendMovieResponse> getTrendingMovies(@PathVariable TimeWindow timeWindow,
                                                      @RequestParam(defaultValue = "1") int page) {
        return tmdbService.getTrendingMovies(timeWindow, page);
    }

    @GetMapping("/day-top")
    public Flux<TrendMovieResponse> getCachedMoviesForDay() {

        return tmdbService.getTrendingMoviesForOneDay();
    }

    @GetMapping("/week-top")
    public Flux<TrendMovieResponse> getCachedMoviesForWeek() {
        return tmdbService.getWeeklyTopMovies();
    }
}
