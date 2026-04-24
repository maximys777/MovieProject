package com.maximys777.project.watchlist.tvshow.dto.response.other;

import lombok.Builder;

@Builder
public record GenreResponse(
        Long id,
        String name
) {
}
