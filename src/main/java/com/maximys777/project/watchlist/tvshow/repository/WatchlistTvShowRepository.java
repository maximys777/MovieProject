package com.maximys777.project.watchlist.tvshow.repository;

import com.maximys777.project.watchlist.tvshow.entity.WatchlistTvShowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchlistTvShowRepository extends JpaRepository<WatchlistTvShowEntity, Long> {
    Optional<WatchlistTvShowEntity> findByUserIdAndTvShowId(Long userId, Long tvShowId);

    Page<WatchlistTvShowEntity> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndTvShowId(Long userId, Long tvShowId);

    void deleteByUserIdAndTvShowId(Long userId, Long tvShowId);
}
