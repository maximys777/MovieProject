package com.maximys777.project.tmdb.dto.response.movie;

import com.maximys777.project.tmdb.dto.response.movie.other.GenreResponse;
import com.maximys777.project.tmdb.dto.response.movie.other.ProductionCountryResponse;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MovieDetails(
        List<GenreResponse> genres,
        String overview,
        String posterPath,
        List<ProductionCountryResponse> productionCountries,
        String releaseDate,
        Integer runtime,
        String title,
        Double voteAverage
) {
}
