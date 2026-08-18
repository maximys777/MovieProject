package com.maximys777.project.watchlist.service.tvshow;

import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.tmdb.service.TMDBService;
import com.maximys777.project.watchlist.tvshow.dto.request.AddToWatchlistTvShowRequest;
import com.maximys777.project.watchlist.tvshow.dto.response.AddedWatchlistTvShowResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.other.GenreResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.other.ProductionCountriesResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.SeasonResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.TvShowDetailsResponse;
import com.maximys777.project.watchlist.tvshow.entity.WatchlistTvShowEntity;
import com.maximys777.project.watchlist.tvshow.mapper.WatchlistTvShowMapper;
import com.maximys777.project.watchlist.tvshow.repository.WatchlistTvShowRepository;
import com.maximys777.project.watchlist.tvshow.service.WatchlistTvShowService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WatchlistTvShowServiceTest {

    @Mock
    private WatchlistTvShowRepository watchlistTvShowRepository;

    @Mock
    private TMDBService tmdbService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WatchlistTvShowMapper watchlistTvShowMapper;

    @Mock
    private OidcUser oidcUser;

    @InjectMocks
    private WatchlistTvShowService watchlistTvShowService;

    @Test
    void addToWatchlistOrUpdateTvShowProgress_ShouldReturnAddedWatchlistTvShowResponse_WhenSuccess() {
        String email = "maximys@gmail.com";
        Mockito.when(oidcUser.getEmail()).thenReturn(email);

        UserEntity user = UserEntity.builder().id(1L).email(email).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100501L)
                .name("Spider-Man")
                .posterUrl("https://www.google.com")
                .firstAirDate("2009-05-12")
                .originCountry("USA")
                .currentSeason(1)
                .currentEpisode(5)
                .build();

        SeasonResponse season = SeasonResponse.builder()
                .seasonNumber(1)
                .episodeCount(6)
                .build();

        TvShowDetailsResponse tmdbResponse = TvShowDetailsResponse.builder()
                .id(request.tvShowId())
                .seasons(List.of(season))
                .build();

        WatchlistTvShowEntity savedEntity = WatchlistTvShowEntity.builder()
                .id(10L)
                .tvShowId(request.tvShowId())
                .userId(user.getId())
                .build();

        AddedWatchlistTvShowResponse expectedResponse = AddedWatchlistTvShowResponse.builder()
                .id(10L)
                .name(request.name())
                .build();

        Mockito.when(userRepository.findByEmail(oidcUser.getEmail()))
                .thenReturn(Optional.of(user));

        Mockito.when(tmdbService.getTvShowDetails(request.tvShowId()))
                .thenReturn(Mono.just(tmdbResponse));

        Mockito.when(watchlistTvShowRepository.findByUserIdAndTvShowId(user.getId(), request.tvShowId()))
                .thenReturn(Optional.empty());

        Mockito.when(watchlistTvShowRepository.save(Mockito.any(WatchlistTvShowEntity.class)))
                .thenReturn(savedEntity);

        Mockito.when(watchlistTvShowMapper.mapToAddedWatchlistTvShow(Mockito.any(WatchlistTvShowEntity.class)))
                .thenReturn(expectedResponse);

        AddedWatchlistTvShowResponse actualResponse = watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(expectedResponse.id(), actualResponse.id());

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(oidcUser.getEmail());
        Mockito.verify(tmdbService, Mockito.times(1)).getTvShowDetails(request.tvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1)).findByUserIdAndTvShowId(user.getId(), request.tvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1)).save(Mockito.any(WatchlistTvShowEntity.class));
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_ShouldUpdateWatchlistTvShowResponse_WhenSuccess() {
        String email = "maximys@gmail.com";
        Mockito.when(oidcUser.getEmail()).thenReturn(email);

        UserEntity user = UserEntity.builder().email(email).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100501L)
                .name("Spider-Man")
                .posterUrl("https://www.google.com")
                .firstAirDate("2009-05-12")
                .originCountry("USA")
                .currentSeason(1)
                .currentEpisode(6)
                .build();

        SeasonResponse season = SeasonResponse.builder()
                .seasonNumber(1)
                .episodeCount(6)
                .build();

        TvShowDetailsResponse tmdbResponse = TvShowDetailsResponse.builder()
                .id(request.tvShowId())
                .seasons(List.of(season))
                .build();

        WatchlistTvShowEntity existingEntity = WatchlistTvShowEntity.builder()
                .id(10L)
                .tvShowId(request.tvShowId())
                .userId(user.getId())
                .currentSeason(1)
                .currentEpisode(5)
                .build();

        AddedWatchlistTvShowResponse expectedResponse = AddedWatchlistTvShowResponse.builder()
                .id(10L)
                .name(request.name())
                .currentSeason(request.currentSeason())
                .currentEpisode(request.currentEpisode())
                .build();

        Mockito.when(userRepository.findByEmail(oidcUser.getEmail()))
                .thenReturn(Optional.of(user));

        Mockito.when(tmdbService.getTvShowDetails(request.tvShowId()))
                .thenReturn(Mono.just(tmdbResponse));

        Mockito.when(watchlistTvShowRepository.findByUserIdAndTvShowId(user.getId(), request.tvShowId()))
                .thenReturn(Optional.of(existingEntity));

        Mockito.when(watchlistTvShowRepository.save(Mockito.any(WatchlistTvShowEntity.class)))
                .thenAnswer(invocation -> {
                    WatchlistTvShowEntity entityToSave = invocation.getArgument(0);

                    Assertions.assertEquals(request.currentSeason(), entityToSave.getCurrentSeason());
                    Assertions.assertEquals(request.currentEpisode(), entityToSave.getCurrentEpisode());
                    return entityToSave;
                });

        Mockito.when(watchlistTvShowMapper.mapToAddedWatchlistTvShow(Mockito.any(WatchlistTvShowEntity.class)))
                .thenReturn(expectedResponse);

        AddedWatchlistTvShowResponse actualResponse = watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(expectedResponse.id(), actualResponse.id());
        Assertions.assertEquals(expectedResponse.currentSeason(), actualResponse.currentSeason());
        Assertions.assertEquals(expectedResponse.currentEpisode(), actualResponse.currentEpisode());

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(oidcUser.getEmail());
        Mockito.verify(tmdbService, Mockito.times(1)).getTvShowDetails(request.tvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1)).findByUserIdAndTvShowId(user.getId(), request.tvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1)).save(Mockito.any(WatchlistTvShowEntity.class));
        Mockito.verify(watchlistTvShowMapper, Mockito.times(1)).mapToAddedWatchlistTvShow(Mockito.any(WatchlistTvShowEntity.class));
    }
}
