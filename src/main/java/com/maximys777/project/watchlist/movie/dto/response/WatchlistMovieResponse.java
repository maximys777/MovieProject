package com.maximys777.project.watchlist.movie.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record WatchlistMovieResponse(
        Long id,
        String posterUrl,
        String title,
        String overview,
        Integer runtime,
        Long movieId,
        LocalDateTime releaseDate,
        Double voteAverage,
        Long userId,
        List<String> genres,
        List<String> productionCountries
) {
}
