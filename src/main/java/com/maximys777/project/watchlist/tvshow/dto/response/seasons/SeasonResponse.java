package com.maximys777.project.watchlist.tvshow.dto.response.seasons;

import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SeasonResponse(
        Long id,
        String name,
        Integer seasonNumber,
        Integer episodeCount,
        String airDate
) {
}