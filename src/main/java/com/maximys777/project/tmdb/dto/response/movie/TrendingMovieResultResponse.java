package com.maximys777.project.tmdb.dto.response.movie;

import lombok.Builder;

import java.util.List;

@Builder
public record TrendingMovieResultResponse(
        Boolean adult,
        String backdrop_path,
        Integer id,
        String title,
        String original_language,
        String original_title,
        String overview,
        String poster_path,
        String media_type,
        List<Integer> genre_ids,
        Number popularity,
        String release_date,
        Boolean video,
        Number vote_average,
        Integer vote_count
) {
}
