package com.maximys777.project.watchlist.tvshow.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.watchlist.tvshow.dto.request.AddToWatchlistTvShowRequest;
import com.maximys777.project.watchlist.tvshow.dto.response.AddedWatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.WatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.service.WatchlistTvShowService;
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
import reactor.core.publisher.Mono;

@Tag(name = "Watchlist TV Shows", description = "Endpoints for managing TV show watchlist")
@RestController
@RequestMapping("/watchlist-tv-shows")
@RequiredArgsConstructor
public class WatchlistTvShowController {
    private final WatchlistTvShowService watchlistTvShowService;

    @Operation(summary = "Add/update TV show in watchlist", description = "Adds a TV show to watchlist or updates progress for existing entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "TV show added/updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (e.g., invalid episode number)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "TV show not found, season not exists, or user not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddedWatchlistTvShowResponse addToWatchlistTvShow(@RequestBody AddToWatchlistTvShowRequest request,
                                                             @AuthenticationPrincipal OidcUser oidcUser) {
        return watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser);
    }

    @Operation(summary = "Get user's watchlist TV shows", description = "Returns paginated watchlist TV shows for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)")
    })
    @GetMapping("/{userId}/user")
    public Mono<Page<WatchlistTvShowResponse>> getUsersWatchlistTvShows(@PathVariable Long userId,
                                                                        Pageable pageable,
                                                                        @RequestParam LanguageType language) {
        return watchlistTvShowService.findUsersWatchlistTvShow(userId, pageable, language);
    }

    @Operation(summary = "Get current user's watchlist TV shows", description = "Returns paginated watchlist TV shows for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)")
    })
    @GetMapping("/me")
    public Mono<Page<WatchlistTvShowResponse>> getMyWatchlistTvShows(@AuthenticationPrincipal OidcUser oidcUser,
                                                                     Pageable pageable,
                                                                     @RequestParam LanguageType language) {
        return watchlistTvShowService.getAuthenticatedUserWatchlist(oidcUser, pageable, language);
    }

    @Operation(summary = "Remove TV show from watchlist", description = "Removes a TV show from the authenticated user's watchlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "TV show removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid TV show ID supplied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "TV show not found in watchlist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping()
    public void deleteTvShowFromWatchlist(@AuthenticationPrincipal OidcUser oidcUser,
                                          @RequestParam Long tvShowId) {
        watchlistTvShowService.deleteTvShowFromWatchlist(oidcUser, tvShowId);
    }
}
