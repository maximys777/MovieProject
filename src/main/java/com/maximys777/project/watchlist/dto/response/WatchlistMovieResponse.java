package com.maximys777.project.watchlist.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistMovieResponse(
        Long id,
        String posterUrl,
        String title,
        Long movieId,
        LocalDateTime releaseDate,
        BigDecimal popularity,
        Long userId
) {
}
