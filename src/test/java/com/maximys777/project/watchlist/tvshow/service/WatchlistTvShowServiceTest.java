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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WatchlistTvShowServiceTest {

    private static final String EMAIL = "test@gmail.com";
    private static final Long USER_ID = 1L;
    private static final Long TV_SHOW_ID = 1005113L;
    private static final Long ENTITY_ID = 1L;

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

    @Captor
    private ArgumentCaptor<WatchlistTvShowEntity> argumentCaptor;

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldReturnAddedWatchlistTvShowResponse_whenSuccess() {
        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);

        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(TV_SHOW_ID)
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
                .id(ENTITY_ID)
                .tvShowId(request.tvShowId())
                .userId(user.getId())
                .build();

        AddedWatchlistTvShowResponse expectedResponse = AddedWatchlistTvShowResponse.builder()
                .id(1L)
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
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .findByUserIdAndTvShowId(user.getId(), request.tvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .save(argumentCaptor.capture());

        WatchlistTvShowEntity captured = argumentCaptor.getValue();

        Assertions.assertEquals(TV_SHOW_ID, captured.getTvShowId());
        Assertions.assertEquals(USER_ID, captured.getUserId());
        Assertions.assertNull(captured.getId());
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldUpdateWatchlistTvShowResponse_whenSuccess() {
        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);

        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(TV_SHOW_ID)
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
                .id(ENTITY_ID)
                .tvShowId(request.tvShowId())
                .userId(user.getId())
                .currentSeason(1)
                .currentEpisode(5)
                .build();

        AddedWatchlistTvShowResponse expectedResponse = AddedWatchlistTvShowResponse.builder()
                .id(ENTITY_ID)
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
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .findByUserIdAndTvShowId(user.getId(), request.tvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .save(argumentCaptor.capture());
        Mockito.verify(watchlistTvShowMapper, Mockito.times(1)).mapToAddedWatchlistTvShow(Mockito.any(WatchlistTvShowEntity.class));

        WatchlistTvShowEntity captured = argumentCaptor.getValue();

        Assertions.assertEquals(TV_SHOW_ID, captured.getTvShowId());
        Assertions.assertEquals(USER_ID, captured.getUserId());
        Assertions.assertEquals(1, captured.getCurrentSeason());
        Assertions.assertEquals(6, captured.getCurrentEpisode());
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldThrowUserNotFoundException_whenUserDoesntExist() {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(TV_SHOW_ID)
                .name("Spider-Man")
                .posterUrl("https://www.google.com")
                .firstAirDate("2009-05-12")
                .originCountry("USA")
                .currentSeason(1)
                .currentEpisode(6)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser));

        Assertions.assertEquals("User not found", exception.getMessage());

        Mockito.verify(watchlistTvShowRepository, Mockito.times(0))
                .save(Mockito.any(WatchlistTvShowEntity.class));
        Mockito.verify(watchlistTvShowRepository, Mockito.times(0))
                .findByUserIdAndTvShowId(USER_ID, TV_SHOW_ID);
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldThrowTvShowNotExistsException_whenTmdbReturnNull() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(TV_SHOW_ID)
                .name("Spider-Man")
                .posterUrl("https://www.google.com")
                .firstAirDate("2009-05-12")
                .originCountry("USA")
                .currentSeason(1)
                .currentEpisode(6)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        Mockito.when(tmdbService.getTvShowDetails(TV_SHOW_ID))
                .thenReturn(Mono.empty());

        TvShowNotExistsException exception = Assertions.assertThrows(
                TvShowNotExistsException.class,
                () -> watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser));

        Assertions.assertEquals("Tv show " + request.name() + " does not exist", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(TV_SHOW_ID);
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldThrowSeasonNotExistsException_whenSeasonNotFound() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(1001325L)
                .name("World of Warcraft")
                .posterUrl("https://www.google.com")
                .firstAirDate("2020-02-25")
                .originCountry("United States")
                .currentSeason(5)
                .currentEpisode(1)
                .build();

        TvShowDetailsResponse detailsResponse = TvShowDetailsResponse.builder()
                .id(1001325L)
                .name("World of Warcraft")
                .posterPath("https://www.google.com")
                .overview("Overview")
                .voteAverage(8.0)
                .firstAirDate("2020-02-25")
                .numberOfEpisodes(3)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(10L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("USA", "United States")))
                .seasons(List.of(new SeasonResponse(10L, "First season", 1, 3, "2020-02-25")))
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(tmdbService.getTvShowDetails(1001325L))
                .thenReturn(Mono.just(detailsResponse));

        SeasonNotExistsException exception = Assertions.assertThrows(
                SeasonNotExistsException.class,
                () -> watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser)
        );

        Assertions.assertEquals("Season " + request.currentSeason() + " does not exist", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(1001325L);
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldThrowEpisodeNotExistsException_whenEpisodeGreaterThanEpisodeCount() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(1001325L)
                .name("World of Warcraft")
                .posterUrl("https://www.google.com")
                .firstAirDate("2020-02-25")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(5)
                .build();

        SeasonResponse seasonResponse = SeasonResponse.builder()
                .id(10L)
                .name("First Season")
                .seasonNumber(1)
                .episodeCount(3)
                .airDate("2020-02-25")
                .build();

        TvShowDetailsResponse detailsResponse = TvShowDetailsResponse.builder()
                .id(1001325L)
                .name("World of Warcraft")
                .posterPath("https://www.google.com")
                .overview("Overview")
                .voteAverage(8.0)
                .firstAirDate("2020-02-25")
                .numberOfEpisodes(3)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(10L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("USA", "United States")))
                .seasons(List.of(seasonResponse))
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(tmdbService.getTvShowDetails(1001325L))
                .thenReturn(Mono.just(detailsResponse));

        EpisodeNotExistsException exception = Assertions.assertThrows(
                EpisodeNotExistsException.class,
                () -> watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser)
        );

        Assertions.assertEquals("In season " + request.currentSeason() + " only " + seasonResponse.episodeCount() + " episodes", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(1001325L);
    }

    @Test
    void addToWatchlistOrUpdateTvShowProgress_shouldThrowEpisodeNotExistsException_whenEpisodeIsNegative() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(1001325L)
                .name("World of Warcraft")
                .posterUrl("https://www.google.com")
                .firstAirDate("2020-02-25")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(-3)
                .build();

        SeasonResponse seasonResponse = SeasonResponse.builder()
                .id(10L)
                .name("First Season")
                .seasonNumber(1)
                .episodeCount(3)
                .airDate("2020-02-25")
                .build();

        TvShowDetailsResponse detailsResponse = TvShowDetailsResponse.builder()
                .id(1001325L)
                .name("World of Warcraft")
                .posterPath("https://www.google.com")
                .overview("Overview")
                .voteAverage(8.0)
                .firstAirDate("2020-02-25")
                .numberOfEpisodes(3)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(10L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("USA", "United States")))
                .seasons(List.of(seasonResponse))
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(tmdbService.getTvShowDetails(1001325L))
                .thenReturn(Mono.just(detailsResponse));

        EpisodeNotExistsException exception = Assertions.assertThrows(
                EpisodeNotExistsException.class,
                () -> watchlistTvShowService.addToWatchlistOrUpdateTvShowProgress(request, oidcUser)
        );

        Assertions.assertEquals("In season " + request.currentSeason() + " only " + seasonResponse.episodeCount() + " episodes", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(1001325L);
    }

    @Test
    void findUsersWatchlistTvShow_shouldReturnPageWatchlistTvShowResponse_whenUserExists() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        LanguageType languageType = LanguageType.en;

        Pageable pageable = PageRequest.of(0, 10);

        WatchlistTvShowEntity entity = WatchlistTvShowEntity.builder()
                .id(1L)
                .tvShowId(TV_SHOW_ID)
                .userId(USER_ID)
                .build();

        WatchlistTvShowResponse response = WatchlistTvShowResponse.builder()
                .id(1L)
                .posterUrl("/poster.jpg")
                .name("TvShow name")
                .overview("TvShow overview")
                .tvShowId(TV_SHOW_ID)
                .userId(user.getId())
                .build();

        Page<WatchlistTvShowEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        TvShowDetailsResponse detailsResponse = TvShowDetailsResponse.builder()
                .id(1L)
                .name("TvShow name")
                .posterPath("/poster.jpg")
                .overview("TvShow overview")
                .voteAverage(7.0)
                .firstAirDate("2021-01-01")
                .numberOfSeasons(3)
                .numberOfEpisodes(39)
                .genres(List.of(new GenreResponse(1L, "Adventure")))
                .productionCountries(List.of(new ProductionCountriesResponse("1234f", "Sony")))
                .build();

        Mockito.when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistTvShowRepository.findByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getTvShowDetails(entity.getTvShowId()))
                .thenReturn(Mono.just(detailsResponse));

        Page<WatchlistTvShowResponse> actualResponse = watchlistTvShowService
                .findUsersWatchlistTvShow(USER_ID, pageable, languageType);

        Mockito.verify(userRepository, Mockito.times(1))
                .findById(USER_ID);
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .findByUserId(USER_ID, pageable);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(entity.getTvShowId());
        Mockito.verifyNoInteractions(watchlistTvShowMapper);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(1, actualResponse.getTotalElements());
        Assertions.assertEquals(response.id(), actualResponse.getContent().getFirst().id());
        Assertions.assertEquals(response.posterUrl(), actualResponse.getContent().getFirst().posterUrl());
        Assertions.assertEquals(response.name(), actualResponse.getContent().getFirst().name());
        Assertions.assertEquals(response.overview(), actualResponse.getContent().getFirst().overview());
        Assertions.assertEquals(TV_SHOW_ID, actualResponse.getContent().getFirst().tvShowId());
        Assertions.assertEquals(USER_ID, actualResponse.getContent().getFirst().userId());
    }

    @Test
    void findUsersWatchlistTvShow_shouldThrowUsernameNotFoundException_whenUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        LanguageType languageType = LanguageType.en;

        Mockito.when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistTvShowService.findUsersWatchlistTvShow(USER_ID, pageable, languageType));

        Assertions.assertEquals("User " + USER_ID + " does not exist", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findById(USER_ID);
    }

    @Test
    void findUsersWatchlistTvShow_shouldThrowMapper_whenTMDBReturnNull() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        LanguageType languageType = LanguageType.en;

        Pageable pageable = PageRequest.of(0, 10);

        WatchlistTvShowEntity entity = WatchlistTvShowEntity.builder()
                .id(1L)
                .tvShowId(TV_SHOW_ID)
                .userId(USER_ID)
                .build();

        WatchlistTvShowResponse fallbackResponse = WatchlistTvShowResponse.builder()
                .id(1L)
                .posterUrl("/poster.jpg")
                .name("TvShow name")
                .overview("TvShow overview")
                .tvShowId(TV_SHOW_ID)
                .userId(user.getId())
                .build();

        Page<WatchlistTvShowEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        Mockito.when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistTvShowRepository.findByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getTvShowDetails(entity.getTvShowId()))
                .thenReturn(Mono.empty());
        Mockito.when(watchlistTvShowMapper.mapToWatchlistTvShowResponse(entity, languageType))
                .thenReturn(fallbackResponse);

        Page<WatchlistTvShowResponse> actualResponse = watchlistTvShowService
                .findUsersWatchlistTvShow(USER_ID, pageable, languageType);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(1, actualResponse.getTotalElements());
        Assertions.assertEquals(fallbackResponse.id(), actualResponse.getContent().getFirst().id());
        Assertions.assertEquals(fallbackResponse.posterUrl(), actualResponse.getContent().getFirst().posterUrl());
        Assertions.assertEquals(fallbackResponse.name(), actualResponse.getContent().getFirst().name());
        Assertions.assertEquals(fallbackResponse.overview(), actualResponse.getContent().getFirst().overview());
        Assertions.assertEquals(fallbackResponse.tvShowId(), actualResponse.getContent().getFirst().tvShowId());
        Assertions.assertEquals(fallbackResponse.userId(), actualResponse.getContent().getFirst().userId());

        Mockito.verify(userRepository, Mockito.times(1))
                .findById(USER_ID);
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .findByUserId(USER_ID, pageable);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(entity.getTvShowId());
        Mockito.verify(watchlistTvShowMapper, Mockito.times(1))
                .mapToWatchlistTvShowResponse(entity, languageType);
    }

    @Test
    void getAuthenticatedUserWatchlist_shouldReturnPageWatchlistTvShowResponse_whenUserAuthenticated() {
        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);

        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        LanguageType languageType = LanguageType.en;

        Pageable pageable = PageRequest.of(0, 10);

        WatchlistTvShowEntity entity = WatchlistTvShowEntity.builder()
                .id(1L)
                .tvShowId(TV_SHOW_ID)
                .userId(USER_ID)
                .build();

        Page<WatchlistTvShowEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        TvShowDetailsResponse detailsResponse = TvShowDetailsResponse.builder()
                .id(1L)
                .name("TvShow name")
                .posterPath("/poster.jpg")
                .overview("TvShow overview")
                .voteAverage(7.0)
                .firstAirDate("2021-01-01")
                .numberOfSeasons(3)
                .numberOfEpisodes(39)
                .genres(List.of(new GenreResponse(1L, "Adventure")))
                .productionCountries(List.of(new ProductionCountriesResponse("1234f", "Sony")))
                .build();

        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistTvShowRepository.findByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getTvShowDetails(entity.getTvShowId()))
                .thenReturn(Mono.just(detailsResponse));

        Page<WatchlistTvShowResponse> actualResponse = watchlistTvShowService
                .getAuthenticatedUserWatchlist(oidcUser, pageable, languageType);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(1, actualResponse.getTotalElements());
        Assertions.assertEquals(detailsResponse.id(), actualResponse.getContent().getFirst().id());
        Assertions.assertEquals(detailsResponse.name(), actualResponse.getContent().getFirst().name());
        Assertions.assertEquals(detailsResponse.posterPath(), actualResponse.getContent().getFirst().posterUrl());
        Assertions.assertEquals(detailsResponse.overview(), actualResponse.getContent().getFirst().overview());
        Assertions.assertEquals(detailsResponse.voteAverage(), actualResponse.getContent().getFirst().voteAverage());
        Assertions.assertEquals(detailsResponse.firstAirDate(), actualResponse.getContent().getFirst().firstAirDate());
        Assertions.assertEquals(detailsResponse.genres().getFirst().name(), actualResponse.getContent().getFirst().genres().getFirst());
        Assertions.assertEquals(detailsResponse.productionCountries().getFirst().name(), actualResponse.getContent().getFirst().productionCountries().getFirst());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .findByUserId(USER_ID, pageable);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(entity.getTvShowId());
        Mockito.verifyNoInteractions(watchlistTvShowMapper);
    }

    @Test
    void getAuthenticatedUserWatchlist_shouldThrowUserNotFoundException_whenUserNotAuthenticated() {
        LanguageType languageType = LanguageType.en;

        Pageable pageable = PageRequest.of(0, 10);

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistTvShowService.getAuthenticatedUserWatchlist(oidcUser, pageable, languageType));

        Assertions.assertEquals("User not found", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verifyNoInteractions(watchlistTvShowRepository, watchlistTvShowMapper);
    }

    @Test
    void getAuthenticatedUserWatchlist_shouldThrowMapper_whenTMDBReturnNull() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        LanguageType languageType = LanguageType.en;

        Pageable pageable = PageRequest.of(0, 10);

        WatchlistTvShowEntity entity = WatchlistTvShowEntity.builder()
                .id(1L)
                .tvShowId(TV_SHOW_ID)
                .userId(USER_ID)
                .build();

        WatchlistTvShowResponse fallbackResponse = WatchlistTvShowResponse.builder()
                .id(1L)
                .posterUrl("/poster.jpg")
                .name("TvShow name")
                .overview("TvShow overview")
                .tvShowId(TV_SHOW_ID)
                .userId(user.getId())
                .build();

        Page<WatchlistTvShowEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistTvShowRepository.findByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getTvShowDetails(entity.getTvShowId()))
                .thenReturn(Mono.empty());
        Mockito.when(watchlistTvShowMapper.mapToWatchlistTvShowResponse(entity, languageType))
                .thenReturn(fallbackResponse);

        Page<WatchlistTvShowResponse> actualResponse = watchlistTvShowService
                .getAuthenticatedUserWatchlist(oidcUser, pageable, languageType);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(1, actualResponse.getTotalElements());
        Assertions.assertEquals(fallbackResponse.id(), actualResponse.getContent().getFirst().id());
        Assertions.assertEquals(fallbackResponse.posterUrl(), actualResponse.getContent().getFirst().posterUrl());
        Assertions.assertEquals(fallbackResponse.name(), actualResponse.getContent().getFirst().name());
        Assertions.assertEquals(fallbackResponse.overview(), actualResponse.getContent().getFirst().overview());
        Assertions.assertEquals(fallbackResponse.tvShowId(), actualResponse.getContent().getFirst().tvShowId());
        Assertions.assertEquals(fallbackResponse.userId(), actualResponse.getContent().getFirst().userId());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .findByUserId(USER_ID, pageable);
        Mockito.verify(tmdbService, Mockito.times(1))
                .getTvShowDetails(entity.getTvShowId());
        Mockito.verify(watchlistTvShowMapper, Mockito.times(1))
                .mapToWatchlistTvShowResponse(entity, languageType);
    }

    @Test
    void deleteTvShowFromWatchlist_shouldDeleteTvShow_whenTvShowInWatchlist() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        WatchlistTvShowEntity entity = WatchlistTvShowEntity.builder()
                .id(ENTITY_ID)
                .tvShowId(TV_SHOW_ID)
                .userId(USER_ID)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistTvShowRepository.existsByUserIdAndTvShowId(user.getId(), TV_SHOW_ID))
                .thenReturn(true);

        watchlistTvShowService.deleteTvShowFromWatchlist(oidcUser, entity.getTvShowId());

        Assertions.assertEquals(0, watchlistTvShowRepository.count());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .deleteByUserIdAndTvShowId(USER_ID, entity.getTvShowId());
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .existsByUserIdAndTvShowId(user.getId(), TV_SHOW_ID);
    }

    @Test
    void deleteTvShowFromWatchlist_shouldThrowNotFound_whenUserNotAuthenticated() {
        WatchlistTvShowEntity entity = WatchlistTvShowEntity.builder()
                .id(ENTITY_ID)
                .tvShowId(TV_SHOW_ID)
                .userId(USER_ID)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistTvShowService.deleteTvShowFromWatchlist(oidcUser, entity.getTvShowId()));

        Assertions.assertEquals("User not found", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
    }

    @Test
    void deleteTvShowFromWatchlist_shouldThrowTvShowNotFoundException_whenTvShowNotInWatchlist() {
        UserEntity user = UserEntity.builder().id(USER_ID).email(EMAIL).build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistTvShowRepository.existsByUserIdAndTvShowId(user.getId(), TV_SHOW_ID))
                .thenReturn(false);

        TvShowNotFoundException exception = Assertions.assertThrows(
                TvShowNotFoundException.class,
                () -> watchlistTvShowService.deleteTvShowFromWatchlist(oidcUser, TV_SHOW_ID));

        Assertions.assertEquals("Tv show not found in your watchlist", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByEmail(EMAIL);
        Mockito.verify(watchlistTvShowRepository, Mockito.times(1))
                .existsByUserIdAndTvShowId(user.getId(), TV_SHOW_ID);
    }
}
