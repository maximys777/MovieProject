package com.maximys777.project.tmdb.dto.response.tvshow;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TrendTvShowResponse(
        int page,
        List<TvShowResultResponse> results,
        int totalPages
) {
}
