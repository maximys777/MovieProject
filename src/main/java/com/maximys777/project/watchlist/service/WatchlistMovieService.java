package com.maximys777.project.watchlist.service;

import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.watchlist.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.entity.WatchlistMovieEntity;
import com.maximys777.project.watchlist.mapper.WatchlistMovieMapper;
import com.maximys777.project.watchlist.repository.WatchlistMovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchlistMovieService {
    private final WatchlistMovieRepository watchlistMovieRepository;
    private final UserRepository userRepository;
    private final WatchlistMovieMapper watchlistMovieMapper;

    public AddedWatchlistMovieResponse addToWatchlistMovie(AddToWatchlistMovieRequest request, OidcUser oidcUser) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        WatchlistMovieEntity watchlistMovieEntity = WatchlistMovieEntity.builder()
                .posterUrl(request.poster_path())
                .title(request.title())
                .movieId(request.movieId())
                .releaseDate(request.release_date())
                .popularity(request.popularity())
                .userId(user.getId())
                .build();

        WatchlistMovieEntity savedMovie = watchlistMovieRepository.save(watchlistMovieEntity);

        return watchlistMovieMapper.mapToAddedWatchlistMovie(savedMovie);
    }

    // To search user's watchlist for id
    public Page<WatchlistMovieResponse> findUsersWatchlistMovie(Long userId, Pageable pageable) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Page<WatchlistMovieEntity> dtoPage = watchlistMovieRepository.getWatchlistMovieEntityByUserId(user.getId(), pageable);

        return dtoPage.map(watchlistMovieMapper::mapToWatchlistMovie);
    }

    // For authenticated user
    public Page<WatchlistMovieResponse> getAuthenticatedUserWatchlist(OidcUser oidcUser, Pageable pageable) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        Page<WatchlistMovieEntity> dtoPage = watchlistMovieRepository.getWatchlistMovieEntityByUserId(user.getId(), pageable);

        return dtoPage.map(watchlistMovieMapper::mapToWatchlistMovie);
    }

    public void deleteMovieFromWatchlist(OidcUser oidcUser, Long movieId) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        if (!watchlistMovieRepository.existsByMovieIdAndUserId(movieId, user.getId())) {
            throw new IllegalArgumentException("Movie not found in your watchlist");
        }

        watchlistMovieRepository.deleteByUserIdAndMovieId(user.getId(), movieId);
    }

    //TODO create custom exception
    private UserEntity handleUserNotFound(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
