package com.maximys777.project.watchlist.mapper;

import com.maximys777.project.watchlist.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.entity.WatchlistMovieEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WatchlistMovieMapper {
    AddedWatchlistMovieResponse mapToAddedWatchlistMovie(WatchlistMovieEntity watchlistMovie);

    WatchlistMovieResponse mapToWatchlistMovie(WatchlistMovieEntity watchlistMovieEntity);
}
