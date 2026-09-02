package com.maximys777.project.watchlist.movie.service;

import com.maximys777.project.exceptions.exceptions.AlreadyExistsException;
import com.maximys777.project.exceptions.exceptions.MovieNotFoundException;
import com.maximys777.project.exceptions.exceptions.UsernameNotFoundException;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WatchlistMovieServiceTest {

    private static final String EMAIL = "test@gmail.com";
    private static final Long USER_ID = 1L;
    private static final Long MOVIE_ID = 90005L;
    private static final Long ENTITY_ID = 10L;

    @Mock
    private WatchlistMovieRepository watchlistMovieRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WatchlistMovieMapper watchlistMovieMapper;

    @Mock
    private TMDBService tmdbService;

    @Mock
    private OidcUser oidcUser;

    @InjectMocks
    private WatchlistMovieService watchlistMovieService;

    @Captor
    private ArgumentCaptor<WatchlistMovieEntity> entityCaptor;

    @Test
    void addToWatchlistMovie_ShouldReturnAddedWatchlistMovieResponse_WhenSuccess() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .posterPath("path/to/poster")
                .title("Movie title")
                .movieId(MOVIE_ID)
                .releaseDate(LocalDateTime.of(2024, 3, 17, 10, 30, 0))
                .popularity(BigDecimal.valueOf(137.5))
                .build();

        WatchlistMovieEntity savedEntity = WatchlistMovieEntity.builder()
                .id(ENTITY_ID)
                .movieId(MOVIE_ID)
                .userId(USER_ID)
                .build();

        AddedWatchlistMovieResponse expectedResponse = AddedWatchlistMovieResponse.builder()
                .id(ENTITY_ID)
                .title(request.title())
                .movieId(MOVIE_ID)
                .userId(USER_ID)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.existsByMovieIdAndUserId(MOVIE_ID, USER_ID))
                .thenReturn(false);
        Mockito.when(watchlistMovieRepository.save(Mockito.any(WatchlistMovieEntity.class)))
                .thenReturn(savedEntity);
        Mockito.when(watchlistMovieMapper.mapToAddedWatchlistMovie(savedEntity))
                .thenReturn(expectedResponse);

        AddedWatchlistMovieResponse actualResponse =
                watchlistMovieService.addToWatchlistMovie(request, oidcUser);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(ENTITY_ID, actualResponse.id());
        Assertions.assertEquals(MOVIE_ID, actualResponse.movieId());
        Assertions.assertEquals(USER_ID, actualResponse.userId());

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(EMAIL);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .existsByMovieIdAndUserId(MOVIE_ID, USER_ID);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .save(entityCaptor.capture());

        WatchlistMovieEntity captured = entityCaptor.getValue();

        Assertions.assertEquals(MOVIE_ID, captured.getMovieId());
        Assertions.assertEquals(USER_ID, captured.getUserId());
        Assertions.assertNull(captured.getId());
    }

    @Test
    void addToWatchlistMovie_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .movieId(MOVIE_ID)
                .title("Movie title")
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn("non-existent@test.com");
        Mockito.when(userRepository.findByEmail("non-existent@test.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistMovieService.addToWatchlistMovie(request, oidcUser));

        Assertions.assertEquals("User not found", exception.getMessage());

        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .save(Mockito.any(WatchlistMovieEntity.class));
        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .existsByMovieIdAndUserId(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void addToWatchlistMovie_ShouldThrowAlreadyExistsException_WhenMovieAlreadyInWatchlist() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .movieId(MOVIE_ID)
                .title("Movie title")
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Mockito.when(watchlistMovieRepository.existsByMovieIdAndUserId(MOVIE_ID, USER_ID))
                .thenReturn(true);

        AlreadyExistsException exception = Assertions.assertThrows(
                AlreadyExistsException.class,
                () -> watchlistMovieService.addToWatchlistMovie(request, oidcUser));

        Assertions.assertEquals(
                "Movie with id " + MOVIE_ID + " already exists in your watchlist",
                exception.getMessage());

        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .save(Mockito.any(WatchlistMovieEntity.class));
        Mockito.verify(watchlistMovieMapper, Mockito.never())
                .mapToAddedWatchlistMovie(Mockito.any(WatchlistMovieEntity.class));
    }


    @Test
    void findUsersWatchlistMovie_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        LanguageType language = LanguageType.en;

        Mockito.when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistMovieService.findUsersWatchlistMovie(USER_ID, pageable, language));

        Assertions.assertEquals(
                "User not found",
                exception.getMessage());

        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .getWatchlistMovieEntityByUserId(Mockito.anyLong(), Mockito.any(Pageable.class));
    }

    @Test
    void findUsersWatchlistMovie_ShouldReturnPage_WhenTMDBReturnsDetails() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        LanguageType language = LanguageType.en;

        WatchlistMovieEntity entity = WatchlistMovieEntity.builder()
                .id(ENTITY_ID)
                .movieId(MOVIE_ID)
                .userId(USER_ID)
                .build();

        Page<WatchlistMovieEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        MovieDetails movieDetails = new MovieDetails(
                List.of(new GenreResponse(1L, "Action")),
                "Overview",
                ".../poster.jpg",
                List.of(new ProductionCountryResponse("123_SO", "United Kingdom")),
                "2024-03-17",
                124,
                "Title",
                6.0
        );

        Mockito.when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.getWatchlistMovieEntityByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getMovieDetails(MOVIE_ID, language))
                .thenReturn(Mono.just(movieDetails));

        Page<WatchlistMovieResponse> response = watchlistMovieService
                .findUsersWatchlistMovie(USER_ID, pageable, language);

        WatchlistMovieResponse actual = response.getContent().getFirst();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getTotalElements());
        Assertions.assertEquals(ENTITY_ID, actual.id());
        Assertions.assertEquals(MOVIE_ID, actual.movieId());
        Assertions.assertEquals(USER_ID, actual.userId());
        Assertions.assertEquals(movieDetails.title(), actual.title());
        Assertions.assertEquals(movieDetails.overview(), actual.overview());
        Assertions.assertEquals(movieDetails.posterPath(), actual.posterUrl());
        Assertions.assertEquals(movieDetails.runtime(), actual.runtime());
        Assertions.assertEquals(movieDetails.voteAverage(), actual.voteAverage());
        Assertions.assertEquals(List.of("Action"), actual.genres());
        Assertions.assertEquals(List.of("United Kingdom"), actual.productionCountries());

        Mockito.verify(tmdbService, Mockito.times(1))
                .getMovieDetails(MOVIE_ID, language);
        Mockito.verifyNoInteractions(watchlistMovieMapper);
    }

    @Test
    void getAuthenticatedUserWatchlist_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        LanguageType language = LanguageType.en;

        Mockito.when(oidcUser.getEmail()).thenReturn("notfound@gmail.com");
        Mockito.when(userRepository.findByEmail("notfound@gmail.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> watchlistMovieService.getAuthenticatedUserWatchlist(oidcUser, pageable, language));

        Assertions.assertEquals(
                "User not found",
                exception.getMessage());

        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .getWatchlistMovieEntityByUserId(USER_ID, pageable);

        Mockito.verifyNoInteractions(tmdbService, watchlistMovieMapper);
    }

    @Test
    void getAuthenticatedUserWatchlist_ShouldFallbackToMapper_WhenTMDBReturnsNull() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        LanguageType language = LanguageType.en;

        WatchlistMovieEntity entity = WatchlistMovieEntity.builder()
                .id(ENTITY_ID)
                .movieId(MOVIE_ID)
                .userId(USER_ID)
                .build();

        Page<WatchlistMovieEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        WatchlistMovieResponse fallbackResponse = WatchlistMovieResponse.builder()
                .id(1L)
                .posterUrl(".../poster")
                .title("Title movie")
                .overview("Overview movie")
                .runtime(123)
                .movieId(5001L)
                .releaseDate("2021-05-12")
                .voteAverage(4.0)
                .userId(USER_ID)
                .genres(List.of("Action"))
                .productionCountries(List.of("Canada"))
                .addedDate(LocalDateTime.of(2025, 12, 3, 13, 12))
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.getWatchlistMovieEntityByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getMovieDetails(MOVIE_ID, language))
                .thenReturn(Mono.empty());
        Mockito.when(watchlistMovieMapper.mapToWatchlistMovieResponse(entity, language))
                .thenReturn(fallbackResponse);

        Page<WatchlistMovieResponse> actual = watchlistMovieService
                .getAuthenticatedUserWatchlist(oidcUser, pageable, language);

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getTotalElements());
        Assertions.assertSame(fallbackResponse, actual.getContent().getFirst());

        Mockito.verify(tmdbService, Mockito.times(1)).getMovieDetails(MOVIE_ID, language);
        Mockito.verify(watchlistMovieMapper, Mockito.times(1))
                .mapToWatchlistMovieResponse(entity, language);
    }

    @Test
    void getAuthenticatedUserWatchlist_ShouldFallbackToMapper_WhenTMDBThrows() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        LanguageType language = LanguageType.en;

        WatchlistMovieEntity entity = WatchlistMovieEntity.builder()
                .id(ENTITY_ID)
                .movieId(MOVIE_ID)
                .userId(USER_ID)
                .build();

        Page<WatchlistMovieEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        WatchlistMovieResponse fallbackResponse = WatchlistMovieResponse.builder()
                .id(1L)
                .posterUrl(".../poster")
                .title("Title movie")
                .overview("Overview movie")
                .runtime(123)
                .movieId(5001L)
                .releaseDate("2021-05-12")
                .voteAverage(4.0)
                .userId(USER_ID)
                .genres(List.of("Action"))
                .productionCountries(List.of("Canada"))
                .addedDate(LocalDateTime.of(2025, 12, 3, 13, 12))
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.getWatchlistMovieEntityByUserId(USER_ID, pageable))
                .thenReturn(entityPage);
        Mockito.when(tmdbService.getMovieDetails(MOVIE_ID, language))
                .thenReturn(Mono.error(new RuntimeException("TMDB is down")));
        Mockito.when(watchlistMovieMapper.mapToWatchlistMovieResponse(entity, language))
                .thenReturn(fallbackResponse);

        Page<WatchlistMovieResponse> actual = watchlistMovieService
                .getAuthenticatedUserWatchlist(oidcUser, pageable, language);

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.getTotalElements());
        Assertions.assertSame(fallbackResponse, actual.getContent().getFirst());

        Mockito.verify(tmdbService, Mockito.times(1)).getMovieDetails(MOVIE_ID, language);
        Mockito.verify(watchlistMovieMapper, Mockito.times(1))
                .mapToWatchlistMovieResponse(entity, language);
    }

    @Test
    void deleteMovieFromWatchlist_ShouldDeleteMovie_WhenSuccess() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.existsByMovieIdAndUserId(MOVIE_ID, USER_ID))
                .thenReturn(true);

        watchlistMovieService.deleteMovieFromWatchlist(oidcUser, MOVIE_ID);

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(EMAIL);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .existsByMovieIdAndUserId(MOVIE_ID, USER_ID);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .deleteByUserIdAndMovieId(USER_ID, MOVIE_ID);
    }

    @Test
    void deleteMovieFromWatchlist_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        Mockito.when(oidcUser.getEmail()).thenReturn("non-existent@test.com");
        Mockito.when(userRepository.findByEmail("non-existent@test.com"))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                watchlistMovieService.deleteMovieFromWatchlist(oidcUser, MOVIE_ID));

        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .existsByMovieIdAndUserId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .deleteByUserIdAndMovieId(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldThrowMovieNotFoundException_WhenMovieNotFound() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.existsByMovieIdAndUserId(MOVIE_ID, USER_ID))
                .thenReturn(false);

        MovieNotFoundException exception = Assertions.assertThrows(
                MovieNotFoundException.class,
                () -> watchlistMovieService.deleteMovieFromWatchlist(oidcUser, MOVIE_ID));

        Assertions.assertEquals("Movie not found in your watchlist", exception.getMessage());

        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .existsByMovieIdAndUserId(MOVIE_ID, USER_ID);
        Mockito.verify(watchlistMovieRepository, Mockito.never())
                .deleteByUserIdAndMovieId(Mockito.anyLong(), Mockito.anyLong());
    }
}