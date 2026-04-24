package com.maximys777.project.watchlist.movie.mapper;

import com.maximys777.project.watchlist.movie.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WatchlistMovieMapper {
    AddedWatchlistMovieResponse mapToAddedWatchlistMovie(WatchlistMovieEntity watchlistMovie);

    WatchlistMovieResponse mapToWatchlistMovie(WatchlistMovieEntity watchlistMovieEntity);
}
