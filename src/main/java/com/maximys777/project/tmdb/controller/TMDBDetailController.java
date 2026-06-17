package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.dto.response.movie.MovieDetails;
import com.maximys777.project.tmdb.service.TMDBService;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.TvShowDetailsResponse;
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
import reactor.core.publisher.Mono;

@Tag(name = "TMDB Details", description = "Endpoints for retrieving detailed movie and TV show information")
@RestController
@RequiredArgsConstructor
@RequestMapping("/details")
public class TMDBDetailController {
    private final TMDBService tmdbService;

    @Operation(summary = "Get TV show details", description = "Returns detailed information about a TV show by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of TV show details",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TvShowDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "TV show not found",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)",
                    content = @Content)
    })
    @GetMapping("/tv-shows/{id}")
    public Mono<TvShowDetailsResponse> getTvShowDetails(@PathVariable Long id,
                                                        @RequestParam(required = false, defaultValue = "en") LanguageType language) {
        return tmdbService.getTvShowDetails(id, language);
    }

    @Operation(summary = "Get movie details", description = "Returns detailed information about a movie by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of movie details",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovieDetails.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)",
                    content = @Content)
    })
    @GetMapping("/movies/{movieId}")
    public Mono<MovieDetails> getMovieDetails(@PathVariable Long movieId,
                                              @RequestParam(required = false, defaultValue = "en") LanguageType language) {
        return tmdbService.getMovieDetails(movieId, language);
    }
}