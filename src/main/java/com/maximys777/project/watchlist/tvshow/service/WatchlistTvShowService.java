package com.maximys777.project.watchlist.tvshow.service;

import com.maximys777.project.exceptions.exceptions.EpisodeNotExistsException;
import com.maximys777.project.exceptions.exceptions.SeasonNotExistsException;
import com.maximys777.project.exceptions.exceptions.TvShowNotExistsException;
import com.maximys777.project.exceptions.exceptions.TvShowNotFoundException;
import com.maximys777.project.exceptions.exceptions.UsernameNotFoundException;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.service.TMDBService;
import com.maximys777.project.watchlist.tvshow.dto.request.AddToWatchlistTvShowRequest;
import com.maximys777.project.watchlist.tvshow.dto.response.AddedWatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.WatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.other.GenreResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.other.ProductionCountriesResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.SeasonResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.TvShowDetailsResponse;
import com.maximys777.project.watchlist.tvshow.entity.WatchlistTvShowEntity;
import com.maximys777.project.watchlist.tvshow.mapper.WatchlistTvShowMapper;
import com.maximys777.project.watchlist.tvshow.repository.WatchlistTvShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchlistTvShowService {
    private final WatchlistTvShowRepository watchlistTvShowRepository;
    private final TMDBService tmdbService;
    private final UserRepository userRepository;
    private final WatchlistTvShowMapper watchlistTvShowMapper;

    @Transactional
    public AddedWatchlistTvShowResponse addToWatchlistOrUpdateTvShowProgress(AddToWatchlistTvShowRequest request, OidcUser oidcUser) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        TvShowDetailsResponse detailsResponse = tmdbService.getTvShowDetails(request.tvShowId()).block();

        if (detailsResponse == null) {
            throw new TvShowNotExistsException("Tv show " + request.name() + " does not exist");
        }

        SeasonResponse targetSeason = detailsResponse.seasons().stream()
                .filter(s -> Objects.equals(s.seasonNumber(), request.currentSeason()))
                .findFirst()
                .orElseThrow(() -> new SeasonNotExistsException("Season " + request.currentSeason() + " does not exist"));

        if (request.currentEpisode() > targetSeason.episodeCount() || request.currentEpisode() < 0) {
            throw new EpisodeNotExistsException("In season " + request.currentSeason() + " only " + targetSeason.episodeCount() + " episodes");
        }

        Optional<WatchlistTvShowEntity> existingShow = watchlistTvShowRepository.findByUserIdAndTvShowId(user.getId(), request.tvShowId());

        WatchlistTvShowEntity watchlistTvShowEntity;

        if (existingShow.isPresent()) {
            watchlistTvShowEntity = existingShow.get();
            watchlistTvShowEntity.setCurrentSeason(request.currentSeason());
            watchlistTvShowEntity.setCurrentEpisode(request.currentEpisode());
            watchlistTvShowRepository.save(watchlistTvShowEntity);
        } else {
            watchlistTvShowEntity = WatchlistTvShowEntity.builder()
                    .tvShowId(request.tvShowId())
                    .currentSeason(request.currentSeason())
                    .currentEpisode(request.currentEpisode())
                    .userId(user.getId())
                    .build();
            watchlistTvShowRepository.save(watchlistTvShowEntity);
        }

        return watchlistTvShowMapper.mapToAddedWatchlistTvShow(watchlistTvShowEntity);
    }

    public Mono<Page<WatchlistTvShowResponse>> findUsersWatchlistTvShow(Long userId, Pageable pageable, LanguageType language) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User " + userId + " does not exist"));

        return getWatchlistTvShowResponses(pageable, language, user);
    }

    public Mono<Page<WatchlistTvShowResponse>> getAuthenticatedUserWatchlist(OidcUser oidcUser, Pageable pageable, LanguageType language) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        return getWatchlistTvShowResponses(pageable, language, user);
    }

    private Mono<Page<WatchlistTvShowResponse>> getWatchlistTvShowResponses(Pageable pageable,
                                                                            LanguageType language,
                                                                            UserEntity user) {
        Page<WatchlistTvShowEntity> entityPage = watchlistTvShowRepository
                .findByUserId(user.getId(), pageable);

        Flux<WatchlistTvShowEntity> entityFlux = Flux.fromIterable(entityPage.getContent());

        Mono<List<WatchlistTvShowResponse>> responseMono = entityFlux
                .flatMap(entity ->
                        tmdbService.getTvShowDetails(entity.getTvShowId(), language)
                                .map(response -> {
                                    List<String> genres = getGenreFromTvShowDetails(response);
                                    List<String> companies = getCompaniesFromTvShowDetails(response);

                                    return WatchlistTvShowResponse.builder()
                                            .id(entity.getId())
                                            .posterUrl(response.posterPath())
                                            .name(response.name())
                                            .overview(response.overview())
                                            .tvShowId(entity.getTvShowId())
                                            .voteAverage(response.voteAverage())
                                            .firstAirDate(response.firstAirDate())
                                            .genres(genres)
                                            .productionCountries(companies)
                                            .currentSeason(entity.getCurrentSeason())
                                            .currentEpisode(entity.getCurrentEpisode())
                                            .userId(user.getId())
                                            .seasons(response.seasons())
                                            .addedDate(entity.getAddedDate())
                                            .build();
                                })
                                .defaultIfEmpty(watchlistTvShowMapper.mapToWatchlistTvShow(entity)))
                .sort(Comparator.comparing(WatchlistTvShowResponse::addedDate).reversed())
                .collectList();

        return responseMono.map(list -> new PageImpl<>(list, pageable, entityPage.getTotalElements()));
    }

    @Transactional
    public void deleteTvShowFromWatchlist(OidcUser oidcUser, Long tvShowId) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        if (!watchlistTvShowRepository.existsByUserIdAndTvShowId(user.getId(), tvShowId)) {
            throw new TvShowNotFoundException("Tv show not found in your watchlist");
        }

        watchlistTvShowRepository.deleteByUserIdAndTvShowId(user.getId(), tvShowId);
    }


    //TODO create custom exception
    private UserEntity handleUserNotFound(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User  not found"));
    }

    private List<String> getGenreFromTvShowDetails(TvShowDetailsResponse response) {
        return response.genres() != null
                ? response.genres().stream().map(GenreResponse::name).toList()
                : List.of();
    }

    private List<String> getCompaniesFromTvShowDetails(TvShowDetailsResponse response) {
        return response.productionCountries() != null
                ? response.productionCountries().stream().map(ProductionCountriesResponse::name).toList()
                : List.of();
    }
}
