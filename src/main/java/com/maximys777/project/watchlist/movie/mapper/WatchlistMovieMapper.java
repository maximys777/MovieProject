package com.maximys777.project.watchlist.movie.mapper;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.watchlist.movie.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WatchlistMovieMapper {
    AddedWatchlistMovieResponse mapToAddedWatchlistMovie(WatchlistMovieEntity watchlistMovie);

    WatchlistMovieResponse mapToWatchlistMovie(WatchlistMovieEntity watchlistMovieEntity, LanguageType language);

//    WatchlistMovieResponse mapToPageableWatchlistMovie(WatchlistMovieEntity entity, @Context LanguageType language);

    WatchlistMovieResponse mapToWatchlistMovieResponse(WatchlistMovieEntity watchlistMovie, LanguageType language);
}
