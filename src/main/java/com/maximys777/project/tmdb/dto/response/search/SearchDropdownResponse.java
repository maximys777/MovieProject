package com.maximys777.project.tmdb.dto.response.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record SearchDropdownResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("media_type") String mediaType,
        @JsonProperty("title") String title,
        @JsonProperty("poster_url") String posterUrl,
        @JsonProperty("popularity") Double popularity,
        @JsonProperty("release_year") String releaseYear,
        @JsonProperty("genres") List<String> genres
) {
}