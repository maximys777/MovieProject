package com.maximys777.project.watchlist.tvshow.dto.response;

import com.maximys777.project.watchlist.tvshow.dto.response.seasons.SeasonResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record WatchlistTvShowResponse(
        Long id,
        String posterUrl,
        String name,
        String overview,
        Long tvShowId,
        Double voteAverage,
        String firstAirDate,
        List<String> genres,
        List<String> productionCountries,
        Integer currentSeason,
        Integer currentEpisode,
        Long userId,
        List<SeasonResponse> seasons
) {
}
