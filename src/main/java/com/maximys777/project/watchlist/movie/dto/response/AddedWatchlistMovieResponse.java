package com.maximys777.project.watchlist.movie.dto.response;

import lombok.Builder;

@Builder
public record AddedWatchlistMovieResponse(
        Long id,
        String title,
        Long movieId,
        Long userId
) {
}
