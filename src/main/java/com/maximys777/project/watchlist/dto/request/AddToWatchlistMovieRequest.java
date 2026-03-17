package com.maximys777.project.watchlist.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AddToWatchlistMovieRequest(
        String poster_path,
        String title,
        Long movieId,
        LocalDateTime release_date,
        BigDecimal popularity
) {
}
