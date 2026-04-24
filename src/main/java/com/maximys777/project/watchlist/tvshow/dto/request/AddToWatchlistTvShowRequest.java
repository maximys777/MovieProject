package com.maximys777.project.watchlist.tvshow.dto.request;

import lombok.Builder;

@Builder
public record AddToWatchlistTvShowRequest(
        Long tvShowId,
        String name,
        String posterUrl,
        String firstAirDate,
        String originCountry,
        Integer currentSeason,
        Integer currentEpisode
) {
}
