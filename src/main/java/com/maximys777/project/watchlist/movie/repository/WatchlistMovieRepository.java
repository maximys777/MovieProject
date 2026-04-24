package com.maximys777.project.watchlist.movie.repository;

import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchlistMovieRepository extends JpaRepository<WatchlistMovieEntity, Long> {
    Page<WatchlistMovieEntity> getWatchlistMovieEntityByUserId(Long userId, Pageable pageable);

    void deleteByUserIdAndMovieId(Long userId, Long movieId);

    boolean existsByMovieIdAndUserId(Long movieId, Long userId);
}
