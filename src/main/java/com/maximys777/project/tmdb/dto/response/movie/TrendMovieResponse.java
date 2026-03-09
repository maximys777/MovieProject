package com.maximys777.project.tmdb.dto.response.movie;

import lombok.Builder;

import java.util.List;

@Builder
public record TrendMovieResponse(
        int page,
        List<TrendingMovieResultResponse> results
) {
}
