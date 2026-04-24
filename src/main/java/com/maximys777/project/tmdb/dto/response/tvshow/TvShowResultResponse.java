package com.maximys777.project.tmdb.dto.response.tvshow;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TvShowResultResponse(
        Long id,
        String name,
        String overview,
        String originalLanguage,
        String posterPath,
        Number voteAverage,
        String firstAirDate,
        List<String> originCountry
) {
}
