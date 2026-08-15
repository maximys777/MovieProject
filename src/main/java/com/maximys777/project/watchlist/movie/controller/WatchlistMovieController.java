package com.maximys777.project.watchlist.movie.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.watchlist.movie.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.movie.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.service.WatchlistMovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Watchlist Movies", description = "Endpoints for managing movie watchlist")
@RestController
@RequestMapping("/watchlist-movies")
@RequiredArgsConstructor
public class WatchlistMovieController {
    private final WatchlistMovieService watchlistMovieService;

    @Operation(summary = "Add movie to watchlist", description = "Adds a movie to the authenticated user's watchlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movie added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Movie already in watchlist"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddedWatchlistMovieResponse addMovieToWatchlist(@RequestBody AddToWatchlistMovieRequest request,
                                                           @AuthenticationPrincipal OidcUser oidcUser) {
        return watchlistMovieService.addToWatchlistMovie(request, oidcUser);
    }

    @Operation(summary = "Get user's watchlist movies", description = "Returns paginated watchlist movies for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)")
    })
    @GetMapping("/{userId}/user")
    public Page<WatchlistMovieResponse> getWatchlistMovie(@PathVariable Long userId,
                                                                Pageable pageable,
                                                                @RequestParam LanguageType language) {
        return watchlistMovieService.findUsersWatchlistMovie(userId, pageable, language);
    }

    @Operation(summary = "Get current user's watchlist movies", description = "Returns paginated watchlist movies for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)")
    })
    @GetMapping("/me")
    public Page<WatchlistMovieResponse> getMyWatchlistMovie(@AuthenticationPrincipal OidcUser oidcUser,
                                                                  Pageable pageable,
                                                                  @RequestParam LanguageType language) {
        return watchlistMovieService.getAuthenticatedUserWatchlist(oidcUser, pageable, language);
    }

//    @GetMapping("/search/{movieName}")
//    public Page<WatchlistMovieResponse> findMovieInUsersWatchlist(@PathVariable String movieName,
//                                                                  @AuthenticationPrincipal OidcUser oidcUser,
//                                                                  @RequestParam LanguageType language,
//                                                                  Pageable pageable) {
//        return watchlistMovieService.findMovieInUsersWatchlist(oidcUser, movieName, language, pageable);
//    }

    @Operation(summary = "Remove movie from watchlist", description = "Removes a movie from the authenticated user's watchlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Movie removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid movie ID supplied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Movie not found in watchlist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping
    public void deleteMovieFromWatchlist(@AuthenticationPrincipal OidcUser oidcUser,
                                         @RequestParam Long movieId) {
        watchlistMovieService.deleteMovieFromWatchlist(oidcUser, movieId);
    }
}
