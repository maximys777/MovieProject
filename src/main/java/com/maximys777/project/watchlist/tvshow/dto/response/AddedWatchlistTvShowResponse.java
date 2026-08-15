package com.maximys777.project.watchlist.tvshow.dto.response;

import lombok.Builder;

@Builder
public record AddedWatchlistTvShowResponse(
        Long id,
        String name,
        Long tvShowId,
        Long userId,
        Integer currentSeason,
        Integer currentEpisode
) {
}
