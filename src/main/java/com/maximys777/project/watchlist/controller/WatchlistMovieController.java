package com.maximys777.project.watchlist.controller;

import com.maximys777.project.watchlist.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.service.WatchlistMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/watchlist-movies")
@RequiredArgsConstructor
public class WatchlistMovieController {
    private final WatchlistMovieService watchlistMovieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddedWatchlistMovieResponse addMovieToWatchlist(@RequestBody AddToWatchlistMovieRequest request,
                                                           @AuthenticationPrincipal OidcUser oidcUser) {
        return watchlistMovieService.addToWatchlistMovie(request, oidcUser);
    }

    @GetMapping("/{userId}/user")
    public Page<WatchlistMovieResponse> getWatchlistMovie(@PathVariable Long userId,
                                                          Pageable pageable) {
        return watchlistMovieService.findUsersWatchlistMovie(userId, pageable);
    }

    @GetMapping("/me")
    public Page<WatchlistMovieResponse> getMyWatchlistMovie(@AuthenticationPrincipal OidcUser oidcUser,
                                                            Pageable pageable) {
        return watchlistMovieService.getAuthenticatedUserWatchlist(oidcUser, pageable);
    }
}
