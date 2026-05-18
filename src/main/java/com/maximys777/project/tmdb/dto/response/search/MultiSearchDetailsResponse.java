package com.maximys777.project.tmdb.dto.response.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maximys777.project.tmdb.common.MediaType;
import lombok.Builder;

import java.util.List;

@Builder
public record MultiSearchDetailsResponse(
        @JsonProperty("id") Integer id,
        @JsonProperty("title") String title,
        @JsonProperty("overview") String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("media_type") MediaType mediaType,
        @JsonProperty("genre_ids") List<Long> genreIds,
        @JsonProperty("genre_names") List<String> genreNames,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("first_air_date") String firstAirDate,
        @JsonProperty("vote_average") Double voteAverage,
        @JsonProperty("name") String name
) {
}