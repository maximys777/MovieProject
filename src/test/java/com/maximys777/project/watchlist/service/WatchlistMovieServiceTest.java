package com.maximys777.project.watchlist.service;

import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.watchlist.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.dto.response.AddedWatchlistMovieResponse;
import com.maximys777.project.watchlist.dto.response.WatchlistMovieResponse;
import com.maximys777.project.watchlist.entity.WatchlistMovieEntity;
import com.maximys777.project.watchlist.mapper.WatchlistMovieMapper;
import com.maximys777.project.watchlist.repository.WatchlistMovieRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WatchlistMovieServiceTest {
    @Mock
    private WatchlistMovieRepository watchlistMovieRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WatchlistMovieMapper watchlistMovieMapper;

    @InjectMocks
    private WatchlistMovieService watchlistMovieService;

    @Test
    void addToWatchlistMovie_ShouldReturnAddedWatchlistMovieResponse_WhenSuccess() {
        String email = "test@gmail.com";
        OidcUser oidcUser = Mockito.mock(OidcUser.class);

        Mockito.when(oidcUser.getEmail()).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email(email)
                .build();

        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 17, 10, 30, 0);
        BigDecimal popularity = BigDecimal.valueOf(137.5);

        AddToWatchlistMovieRequest request = new AddToWatchlistMovieRequest(
                "path/to/poster",
                "Movie title",
                90005L,
                dateTime,
                popularity);


        WatchlistMovieEntity watchlistMovieEntity = WatchlistMovieEntity.builder()
                .id(10L)
                .posterUrl(request.poster_path())
                .title(request.title())
                .movieId(request.movieId())
                .releaseDate(request.release_date())
                .popularity(request.popularity())
                .build();

        AddedWatchlistMovieResponse expectedResponse = new AddedWatchlistMovieResponse(
                10L,
                "Movie title",
                90005L,
                1L
        );

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.save(Mockito.any(WatchlistMovieEntity.class))).thenReturn(watchlistMovieEntity);
        Mockito.when(watchlistMovieMapper.mapToAddedWatchlistMovie(watchlistMovieEntity)).thenReturn(expectedResponse);

        AddedWatchlistMovieResponse actualResponse = watchlistMovieService.addToWatchlistMovie(request, oidcUser);

        Assertions.assertNotNull(actualResponse);

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(email);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1)).save(Mockito.any(WatchlistMovieEntity.class));
    }

    @Test
    void addToWatchlistMovie_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        OidcUser oidcUser = Mockito.mock(OidcUser.class);
        Mockito.when(oidcUser.getEmail()).thenReturn("non-existent@test.com");

        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 17, 10, 30, 0);
        BigDecimal popularity = BigDecimal.valueOf(137.5);

        AddToWatchlistMovieRequest request = new AddToWatchlistMovieRequest(
                "path/to/poster",
                "Movie title",
                90005L,
                dateTime,
                popularity);

        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                watchlistMovieService.addToWatchlistMovie(request, oidcUser));

        Mockito.verify(watchlistMovieRepository, Mockito.never()).save(Mockito.any(WatchlistMovieEntity.class));
    }

    @Test
    void findUsersWatchlistMovie_ShouldReturnPageableWatchlistMovieResponse_WhenSuccess() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        WatchlistMovieEntity entity = WatchlistMovieEntity.builder()
                .id(10L)
                .build();

        Page<WatchlistMovieEntity> entityPage = new PageImpl<>(List.of(entity));

        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 17, 10, 30, 0);
        BigDecimal popularity = BigDecimal.valueOf(137.5);

        WatchlistMovieResponse expectedResponse = new WatchlistMovieResponse(
                10L,
                "path/to/poster",
                "Movie title",
                90005L,
                dateTime,
                popularity,
                user.getId());

        Mockito.when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.getWatchlistMovieEntityByUserId(user.getId(), pageable))
                .thenReturn(entityPage);
        Mockito.when(watchlistMovieMapper.mapToWatchlistMovie(entity)).thenReturn(expectedResponse);

        Page<WatchlistMovieResponse> actualResponse = watchlistMovieService
                .findUsersWatchlistMovie(user.getId(), pageable);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(1, actualResponse.getTotalElements());
        Assertions.assertEquals(expectedResponse, actualResponse.getContent().getFirst());

        Mockito.verify(userRepository, Mockito.times(1)).findById(user.getId());
        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .getWatchlistMovieEntityByUserId(user.getId(), pageable);
        Mockito.verify(watchlistMovieMapper).mapToWatchlistMovie(entity);
    }

    @Test
    void findUsersWatchlistMovie_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                watchlistMovieService.findUsersWatchlistMovie(1L, pageable));

        Mockito.verify(userRepository, Mockito.times(1)).findById(1L);
    }

    @Test
    void getAuthenticatedUserWatchlist_ShouldReturnPageableWatchlistMovieResponse_WhenSuccess() {
        String email = "test@gmail.com";
        OidcUser oidcUser = Mockito.mock(OidcUser.class);

        Mockito.when(oidcUser.getEmail()).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email(email)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        WatchlistMovieEntity entity = WatchlistMovieEntity.builder()
                .id(10L)
                .build();

        Page<WatchlistMovieEntity> entityPage = new PageImpl<>(List.of(entity));

        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 17, 10, 30, 0);
        BigDecimal popularity = BigDecimal.valueOf(137.5);

        WatchlistMovieResponse expectedResponse = new WatchlistMovieResponse(
                10L,
                "path/to/poster",
                "Movie title",
                90005L,
                dateTime,
                popularity,
                user.getId());

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.getWatchlistMovieEntityByUserId(user.getId(), pageable))
                .thenReturn(entityPage);
        Mockito.when(watchlistMovieMapper.mapToWatchlistMovie(entity)).thenReturn(expectedResponse);

        Page<WatchlistMovieResponse> actualResponse = watchlistMovieService.getAuthenticatedUserWatchlist(oidcUser, pageable);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(1, actualResponse.getTotalElements());
        Assertions.assertEquals(expectedResponse, actualResponse.getContent().getFirst());

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(email);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1))
                .getWatchlistMovieEntityByUserId(user.getId(), pageable);
        Mockito.verify(watchlistMovieMapper).mapToWatchlistMovie(entity);
    }

    @Test
    void getAuthenticatedUserWatchlist_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        OidcUser oidcUser = Mockito.mock(OidcUser.class);
        Pageable pageable = PageRequest.of(0, 10);

        Mockito.when(oidcUser.getEmail()).thenReturn("non-existent@test.com");
        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                watchlistMovieService.getAuthenticatedUserWatchlist(oidcUser, pageable));

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(Mockito.anyString());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldDeleteMovie_WhenSuccess() {
        String email = "test@gmail.com";
        OidcUser oidcUser = Mockito.mock(OidcUser.class);

        Mockito.when(oidcUser.getEmail()).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email(email)
                .build();

        WatchlistMovieEntity watchlistMovieEntity = WatchlistMovieEntity.builder()
                .id(10L)
                .movieId(1L)
                .userId(user.getId())
                .build();

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.existsByMovieIdAndUserId(watchlistMovieEntity.getId(), user.getId())).thenReturn(true);

        watchlistMovieService.deleteMovieFromWatchlist(oidcUser, watchlistMovieEntity.getId());

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(email);
        Mockito.verify(watchlistMovieRepository, Mockito.times(1)).existsByMovieIdAndUserId(watchlistMovieEntity.getId(), user.getId());
        Mockito.verify(watchlistMovieRepository, Mockito.times(1)).deleteByUserIdAndMovieId(user.getId(), watchlistMovieEntity.getId());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        Long movieId = 90005L;
        OidcUser oidcUser = Mockito.mock(OidcUser.class);
        Mockito.when(oidcUser.getEmail()).thenReturn("test@test.com");

        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                watchlistMovieService.deleteMovieFromWatchlist(oidcUser, movieId));

        Mockito.verify(watchlistMovieRepository, Mockito.never()).existsByMovieIdAndUserId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(watchlistMovieRepository, Mockito.never()).deleteByUserIdAndMovieId(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldThrowMovieNotFoundException_WhenMovieNotFound() {
        String email = "test@gmail.com";
        Long movieId = 90005L;
        OidcUser oidcUser = Mockito.mock(OidcUser.class);

        Mockito.when(oidcUser.getEmail()).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email(email)
                .build();

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(watchlistMovieRepository.existsByMovieIdAndUserId(movieId, user.getId())).thenReturn(false);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () ->
                watchlistMovieService.deleteMovieFromWatchlist(oidcUser, movieId));

        Assertions.assertEquals("Movie not found in your watchlist", exception.getMessage());

        Mockito.verify(watchlistMovieRepository, Mockito.times(1)).existsByMovieIdAndUserId(movieId, user.getId());
        Mockito.verify(watchlistMovieRepository, Mockito.never()).deleteByUserIdAndMovieId(Mockito.anyLong(), Mockito.anyLong());
    }
}
