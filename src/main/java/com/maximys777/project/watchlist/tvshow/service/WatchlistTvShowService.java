package com.maximys777.project.watchlist.tvshow.service;

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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new IllegalArgumentException("Tv show " + request.name() + " does not exist");
        }

        SeasonResponse targetSeason = detailsResponse.seasons().stream()
                .filter(s -> Objects.equals(s.seasonNumber(), request.currentSeason()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Season " + request.currentSeason() + " does not exist"));

        if (request.currentEpisode() > targetSeason.episodeCount() || request.currentEpisode() < 0) {
            throw new IllegalArgumentException("In season " + request.currentSeason() + " only " + targetSeason.episodeCount() + " episodes");
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
                    .name(request.name())
                    .posterUrl(request.posterUrl())
                    .firstAirDate(request.firstAirDate())
                    .originCountry(request.originCountry())
                    .currentSeason(request.currentSeason())
                    .currentEpisode(request.currentEpisode())
                    .userId(user.getId())
                    .build();
            watchlistTvShowRepository.save(watchlistTvShowEntity);
        }

        return watchlistTvShowMapper.mapToAddedWatchlistTvShow(watchlistTvShowEntity);
    }

    public Page<WatchlistTvShowResponse> findUsersWatchlistTvShow(Long userId, Pageable pageable, LanguageType language) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User " + userId + " does not exist"));

        return getWatchlistTvShowResponses(pageable, language, user);
    }

    public Page<WatchlistTvShowResponse> getAuthenticatedUserWatchlist(OidcUser oidcUser, Pageable pageable, LanguageType language) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        return getWatchlistTvShowResponses(pageable, language, user);
    }

    private PageImpl<WatchlistTvShowResponse> getWatchlistTvShowResponses(Pageable pageable, LanguageType language, UserEntity user) {
        Page<WatchlistTvShowEntity> dtoPage = watchlistTvShowRepository.findByUserId(user.getId(), pageable);

        List<WatchlistTvShowResponse> translatedTvShows = dtoPage.getContent().stream()
                .map(entity -> {
                    try {
                        TvShowDetailsResponse tmdbShow = tmdbService.getTvShowDetails(entity.getTvShowId(), language).block();
                        if (tmdbShow != null && tmdbShow.name() != null) {

                            List<SeasonResponse> seasonResponse = tmdbShow.seasons().stream()
                                    .filter(s -> s.seasonNumber() > 0)
                                    .map(s -> new SeasonResponse(s.id(), s.name(), s.seasonNumber(), s.episodeCount(), s.airDate()))
                                    .toList();

                            List<String> genreNames = tmdbShow.genres() != null
                                    ? tmdbShow.genres().stream().map(GenreResponse::name).toList()
                                    : List.of();

                            List<String> countryNames = tmdbShow.productionCountries() != null
                                    ? tmdbShow.productionCountries().stream().map(ProductionCountriesResponse::name).toList()
                                    : List.of();

                            return WatchlistTvShowResponse.builder()
                                    .id(entity.getId())
                                    .posterUrl(entity.getPosterUrl())
                                    .name(tmdbShow.name())
                                    .overview(tmdbShow.overview())
                                    .tvShowId(entity.getTvShowId())
                                    .voteAverage(tmdbShow.voteAverage())
                                    .firstAirDate(entity.getFirstAirDate())
                                    .genres(genreNames)
                                    .productionCountries(countryNames)
                                    .currentSeason(entity.getCurrentSeason())
                                    .currentEpisode(entity.getCurrentEpisode())
                                    .userId(user.getId())
                                    .seasons(seasonResponse)
                                    .build();
                        }
                    } catch (Exception e) {
                        System.err.println("ОШИБКА TMDB ДЛЯ СЕРИАЛА " + entity.getName() + ": " + e.getMessage());
                    }

                    return watchlistTvShowMapper.mapToWatchlistTvShowResponse(entity);
                })
                .toList();

        return new PageImpl<>(translatedTvShows, pageable, dtoPage.getTotalElements());
    }

    @Transactional
    public void deleteTvShowFromWatchlist(OidcUser oidcUser, Long tvShowId) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        if (!watchlistTvShowRepository.existsByUserIdAndTvShowId(user.getId(), tvShowId)) {
            throw new IllegalArgumentException("Tv show not found in your watchlist");
        }

        watchlistTvShowRepository.deleteByUserIdAndTvShowId(user.getId(), tvShowId);
    }


    //TODO create custom exception
    private UserEntity handleUserNotFound(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User  not found"));
    }
}
