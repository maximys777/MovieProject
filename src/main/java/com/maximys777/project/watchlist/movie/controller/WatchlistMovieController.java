package com.maximys777.project.watchlist.movie.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.watchlist.movie.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.movie.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.service.WatchlistMovieService;
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
    public Mono<Page<WatchlistMovieResponse>> getWatchlistMovie(@PathVariable Long userId,
                                                                Pageable pageable,
                                                                @RequestParam LanguageType language) {
        return watchlistMovieService.findUsersWatchlistMovie(userId, pageable, language);
    }

    @GetMapping("/me")
    public Mono<Page<WatchlistMovieResponse>> getMyWatchlistMovie(@AuthenticationPrincipal OidcUser oidcUser,
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

    @DeleteMapping
    public void deleteMovieFromWatchlist(@AuthenticationPrincipal OidcUser oidcUser,
                                         @RequestParam Long movieId) {
        watchlistMovieService.deleteMovieFromWatchlist(oidcUser, movieId);
    }
}
