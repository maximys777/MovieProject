package com.maximys777.project.watchlist.tvshow.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.watchlist.tvshow.dto.request.AddToWatchlistTvShowRequest;
import com.maximys777.project.watchlist.tvshow.dto.response.AddedWatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.WatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.service.WatchlistTvShowService;
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

@RestController
@RequestMapping("/watchlist-tv-shows")
@RequiredArgsConstructor
public class WatchlistTvShowController {
    private final WatchlistTvShowService watchlistTvShowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddedWatchlistTvShowResponse addToWatchlistTvShow(@RequestBody AddToWatchlistTvShowRequest request,
                                                             @AuthenticationPrincipal OidcUser oidcUser) {
        return watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser);
    }

    @GetMapping("/{userId}/user")
    public Page<WatchlistTvShowResponse> getUsersWatchlistTvShows(@PathVariable Long userId,
                                                                  Pageable pageable,
                                                                  @RequestParam LanguageType language) {
        return watchlistTvShowService.findUsersWatchlistTvShow(userId, pageable, language);
    }

    @GetMapping("/me")
    public Page<WatchlistTvShowResponse> getMyWatchlistTvShows(@AuthenticationPrincipal OidcUser oidcUser,
                                                               Pageable pageable,
                                                               @RequestParam LanguageType language) {
        return watchlistTvShowService.getAuthenticatedUserWatchlist(oidcUser, pageable, language);
    }

    @DeleteMapping()
    public void deleteTvShowFromWatchlist(@AuthenticationPrincipal OidcUser oidcUser,
                                          @RequestParam Long tvShowId) {
        watchlistTvShowService.deleteTvShowFromWatchlist(oidcUser, tvShowId);
    }
}
