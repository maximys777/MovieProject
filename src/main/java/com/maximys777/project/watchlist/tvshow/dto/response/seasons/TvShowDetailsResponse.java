package com.maximys777.project.watchlist.tvshow.dto.response.seasons;

import com.maximys777.project.watchlist.tvshow.dto.response.other.GenreResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.other.ProductionCountriesResponse;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TvShowDetailsResponse(
        Long id,
        Long tvShowId,
        String name,
        String overview,
        Double voteAverage,
        Integer numberOfEpisodes,
        Integer numberOfSeasons,
        List<GenreResponse> genres,
        List<ProductionCountriesResponse> productionCountries,
        List<SeasonResponse> seasons
) {
}
