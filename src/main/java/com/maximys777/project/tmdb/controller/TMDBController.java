package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.common.TimeWindow;
import com.maximys777.project.tmdb.dto.response.movie.TrendMovieResponse;
import com.maximys777.project.tmdb.dto.response.tvshow.TrendTvShowResponse;
import com.maximys777.project.tmdb.service.TMDBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "TMDB Trending", description = "Endpoints for retrieving trending movies and TV shows")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trending")
public class TMDBController {
    private final TMDBService tmdbService;

    @Operation(summary = "Get trending movies", description = "Returns a list of trending movies for a given time window, page, and language.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of trending movies",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TrendMovieResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)",
                    content = @Content)
    })
    @GetMapping("/movies/{timeWindow}")
    public Flux<TrendMovieResponse> getTrendingMovies(@PathVariable TimeWindow timeWindow,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam LanguageType language) {
        return tmdbService.getTrendingMovies(timeWindow, page, language);
    }

    @Operation(summary = "Get trending TV shows", description = "Returns a list of trending TV shows for a given time window, page, and language.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of trending TV shows",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TrendTvShowResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)",
                    content = @Content)
    })
    @GetMapping("/tv-shows/{timeWindow}")
    public Flux<TrendTvShowResponse> getTrendingTvShows(@PathVariable TimeWindow timeWindow,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam LanguageType language) {
        return tmdbService.getTrendingTvShows(timeWindow, page, language);
    }
}