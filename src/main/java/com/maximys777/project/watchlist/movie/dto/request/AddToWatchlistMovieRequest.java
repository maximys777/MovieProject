package com.maximys777.project.watchlist.movie.dto.request;

import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AddToWatchlistMovieRequest(
        String posterPath,
        String title,
        Long movieId,
        LocalDateTime releaseDate,
        BigDecimal popularity
) {
}