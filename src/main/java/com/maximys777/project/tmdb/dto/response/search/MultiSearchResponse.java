package com.maximys777.project.tmdb.dto.response.search;

import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder
public record MultiSearchResponse(
        Integer page,
        List<MultiSearchDetailsResponse> results,
        Integer totalPages,
        Integer totalResults
) {
}
