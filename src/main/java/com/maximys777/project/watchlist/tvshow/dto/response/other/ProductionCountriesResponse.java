package com.maximys777.project.watchlist.tvshow.dto.response.other;

import lombok.Builder;

@Builder
public record ProductionCountriesResponse(
        String iso_3166_1,
        String name
) {
}
