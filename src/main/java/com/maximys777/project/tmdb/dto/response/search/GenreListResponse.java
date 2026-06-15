package com.maximys777.project.tmdb.dto.response.search;

import java.util.List;

public record GenreListResponse(
        List<GenreResponse> genres
) {
}
