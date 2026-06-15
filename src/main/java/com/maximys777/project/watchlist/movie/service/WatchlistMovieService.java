package com.maximys777.project.watchlist.movie.service;

import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.dto.response.movie.MovieDetails;
import com.maximys777.project.tmdb.dto.response.movie.other.GenreResponse;
import com.maximys777.project.tmdb.dto.response.movie.other.ProductionCountryResponse;
import com.maximys777.project.tmdb.service.TMDBService;
import com.maximys777.project.watchlist.movie.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.movie.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import com.maximys777.project.watchlist.movie.mapper.WatchlistMovieMapper;
import com.maximys777.project.watchlist.movie.repository.WatchlistMovieRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistMovieService {
    private final WatchlistMovieRepository watchlistMovieRepository;
    private final UserRepository userRepository;
    private final WatchlistMovieMapper watchlistMovieMapper;
    private final TMDBService tmdbService;

    public AddedWatchlistMovieResponse addToWatchlistMovie(AddToWatchlistMovieRequest request, OidcUser oidcUser) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        if (watchlistMovieRepository.existsByMovieIdAndUserId(request.movieId(), user.getId())) {
            throw new RuntimeException("Movie with id " + request.movieId() + " already exists in your watchlist");
        }

        WatchlistMovieEntity watchlistMovieEntity = WatchlistMovieEntity.builder()
                .movieId(request.movieId())
                .userId(user.getId())
                .build();

        WatchlistMovieEntity savedMovie = watchlistMovieRepository.save(watchlistMovieEntity);

        return watchlistMovieMapper.mapToAddedWatchlistMovie(savedMovie);
    }

    // To search user's watchlist for id
    public Mono<Page<WatchlistMovieResponse>> findUsersWatchlistMovie(Long userId, Pageable pageable, LanguageType language) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return getWatchlistMovieResponses(pageable, language, user);
    }

    // For authenticated user
    public Mono<Page<WatchlistMovieResponse>> getAuthenticatedUserWatchlist(OidcUser oidcUser, Pageable pageable, LanguageType language) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        return getWatchlistMovieResponses(pageable, language, user);
    }

    private Mono<Page<WatchlistMovieResponse>> getWatchlistMovieResponses(Pageable pageable,
                                                                          LanguageType language,
                                                                          UserEntity user) {
        Page<WatchlistMovieEntity> entityPage = watchlistMovieRepository
                .getWatchlistMovieEntityByUserId(user.getId(), pageable);

        Flux<WatchlistMovieEntity> entityFlux = Flux.fromIterable(entityPage.getContent());

        Mono<List<WatchlistMovieResponse>> responseMono = entityFlux
                .flatMap(entity ->
                        tmdbService.getMovieDetails(entity.getMovieId(), language)
                                .map(movieDetails -> {
                                    List<String> genres = getGenresFromMovieDetails(movieDetails);
                                    List<String> companies = getCompaniesFromMovieDetails(movieDetails);

                                    return WatchlistMovieResponse.builder()
                                            .id(entity.getId())
                                            .posterUrl(movieDetails.posterPath())
                                            .title(movieDetails.title())
                                            .overview(movieDetails.overview())
                                            .runtime(movieDetails.runtime())
                                            .movieId(entity.getMovieId())
                                            .releaseDate(movieDetails.releaseDate())
                                            .voteAverage(movieDetails.voteAverage())
                                            .userId(user.getId())
                                            .genres(genres)
                                            .productionCountries(companies)
                                            .addedDate(entity.getAddedDate())
                                            .build();
                                })
                                .defaultIfEmpty(watchlistMovieMapper.mapToWatchlistMovieResponse(entity, language)))
                .sort(Comparator.comparing(WatchlistMovieResponse::addedDate).reversed())
                .collectList();

        return responseMono.map(list -> new PageImpl<>(list, pageable, entityPage.getTotalElements()));
    }

    @Transactional
    public void deleteMovieFromWatchlist(OidcUser oidcUser, Long movieId) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        if (!watchlistMovieRepository.existsByMovieIdAndUserId(movieId, user.getId())) {
            throw new IllegalArgumentException("Movie not found in your watchlist");
        }

        watchlistMovieRepository.deleteByUserIdAndMovieId(user.getId(), movieId);
    }

    // TODO WatchlistMovieRepository
//    public Page<WatchlistMovieResponse> findMovieInUsersWatchlist(OidcUser oidcUser, String movieName, LanguageType language, Pageable pageable) {
//        UserEntity user = handleUserNotFound(oidcUser.getEmail());
//
//        Page<WatchlistMovieEntity> entityPage = watchlistMovieRepository.
//                findByUserIdAndTitleContainingIgnoreCase(user.getId(), movieName, pageable);
//
//        return entityPage.map(entity -> watchlistMovieMapper.mapToPageableWatchlistMovie(entity, language));
//    }

    //TODO create custom exception
    private UserEntity handleUserNotFound(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private List<String> getGenresFromMovieDetails(MovieDetails movieDetails) {
        return movieDetails.genres() != null
                ? movieDetails.genres().stream().map(GenreResponse::name).toList()
                : List.of();
    }

    private List<String> getCompaniesFromMovieDetails(MovieDetails movieDetails) {
        return movieDetails.productionCountries() != null
                ? movieDetails.productionCountries().stream().map(ProductionCountryResponse::name).toList()
                : List.of();
    }
}
