package com.maximys777.project.watchlist.movie.mapper;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.watchlist.movie.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Mapper(componentModel = "spring")
public interface WatchlistMovieMapper {
    AddedWatchlistMovieResponse mapToAddedWatchlistMovie(WatchlistMovieEntity watchlistMovie);

    WatchlistMovieResponse mapToWatchlistMovie(WatchlistMovieEntity watchlistMovieEntity);

    WatchlistMovieResponse mapToPageableWatchlistMovie(WatchlistMovieEntity entity, @Context LanguageType language);
}
