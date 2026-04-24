package com.maximys777.project.watchlist.tvshow.mapper;

import com.maximys777.project.watchlist.tvshow.dto.response.AddedWatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.WatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.entity.WatchlistTvShowEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WatchlistTvShowMapper {
    AddedWatchlistTvShowResponse mapToAddedWatchlistTvShow(WatchlistTvShowEntity watchlistTvShowEntity);

    WatchlistTvShowResponse mapToWatchlistTvShowResponse(WatchlistTvShowEntity watchlistTvShowEntity);
}
