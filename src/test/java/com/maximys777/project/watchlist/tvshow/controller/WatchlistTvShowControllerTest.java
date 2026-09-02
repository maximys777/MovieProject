package com.maximys777.project.watchlist.tvshow.controller;

import com.maximys777.project.AbstractIntegrationTest;
import com.maximys777.project.exceptions.exceptions.ServiceUnavailable;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.watchlist.tvshow.dto.request.AddToWatchlistTvShowRequest;
import com.maximys777.project.watchlist.tvshow.dto.response.other.GenreResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.other.ProductionCountriesResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.SeasonResponse;
import com.maximys777.project.watchlist.tvshow.dto.response.seasons.TvShowDetailsResponse;
import com.maximys777.project.watchlist.tvshow.entity.WatchlistTvShowEntity;
import com.maximys777.project.watchlist.tvshow.repository.WatchlistTvShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WatchlistTvShowControllerTest extends AbstractIntegrationTest {
    private static final String EMAIL = "test@gmail.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WatchlistTvShowRepository watchlistTvShowRepository;

    private UserEntity user;

    @BeforeEach
    void setUpUser() {
        user = userRepository.save(UserEntity.builder()
                .email(EMAIL)
                .googleId("google-123")
                .build());
    }

    @Test
    void addToWatchlistTvShow_shouldAddedTvShow_whenTvShowNotInWatchlistYet() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(2)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 2, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tvShowId").value(100500123))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.currentSeason").value(1))
                .andExpect(jsonPath("$.currentEpisode").value(1));

        List<WatchlistTvShowEntity> savedTvShows = watchlistTvShowRepository.findAll();

        assertThat(savedTvShows).hasSize(1);
        assertThat(savedTvShows.getFirst().getTvShowId()).isEqualTo(100500123L);
        assertThat(savedTvShows.getFirst().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void addToWatchlistTvShow_shouldUpdateTvShow_whenTvShowInWatchlist() throws Exception {
        watchlistTvShowRepository.save(WatchlistTvShowEntity.builder()
                .tvShowId(100500123L)
                .currentSeason(1)
                .currentEpisode(1)
                .userId(user.getId())
                .build());

        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(2)
                .build();

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(2)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 2, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tvShowId").value(100500123))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.currentSeason").value(1))
                .andExpect(jsonPath("$.currentEpisode").value(2));

        List<WatchlistTvShowEntity> savedTvShows = watchlistTvShowRepository.findAll();

        assertThat(savedTvShows).hasSize(1);
        assertThat(savedTvShows.getFirst().getTvShowId()).isEqualTo(100500123L);
        assertThat(savedTvShows.getFirst().getUserId()).isEqualTo(user.getId());
        assertThat(savedTvShows.getFirst().getCurrentEpisode()).isEqualTo(2);
    }

    @Test
    void addToWatchlistTvShow_shouldReturnUnauthorized_whenUserIsUnauthenticated() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(2)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 2, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(watchlistTvShowRepository.findAll()).hasSize(0);
    }

    @Test
    void addToWatchlistTvShow_shouldReturnNotFound_whenUserNotFoundInDatabase() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(2)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 2, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(authenticatedUser("random@gmail.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));

        assertThat(watchlistTvShowRepository.findAll()).hasSize(0);
    }

    @Test
    void addToWatchlistTvShow_shouldReturnNotFound_whenTvShowDoesNotExistsInTMDB() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.empty());

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tv show " + request.name() + " does not exist"));

        assertThat(watchlistTvShowRepository.findAll()).hasSize(0);
    }

    @Test
    void addToWatchlistTvShow_shouldReturnNotFound_whenSeasonDoesNotExist() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(9)
                .currentEpisode(1)
                .build();

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(1)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 1, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Season " + request.currentSeason() + " does not exist"));

        assertThat(watchlistTvShowRepository.findAll()).hasSize(0);
    }

    @Test
    void addToWatchlistTvShow_shouldReturnServiceUnavailable_whenTMDBNotAvailable() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(100500123L)
                .name("Marvels")
                .posterUrl("/poster.jpg")
                .firstAirDate("2026-09-01")
                .originCountry("United States")
                .currentSeason(9)
                .currentEpisode(1)
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenThrow(new ServiceUnavailable("Service Unavailable"));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());

        assertThat(watchlistTvShowRepository.findAll()).hasSize(0);
    }

    @Test
    void getUsersWatchlistTvShows_shouldReturnUsersWatchlist_whenUserExist() throws Exception {
        watchlistTvShowRepository.save(WatchlistTvShowEntity.builder()
                .tvShowId(100500123L)
                .currentSeason(1)
                .currentEpisode(1)
                .userId(user.getId())
                .build());

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(1)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 1, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(get("/watchlist-tv-shows/{userId}/user", user.getId())
                        .param("language", "en")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value(details.name()))
                .andExpect(jsonPath("$.content[0].posterUrl").value(details.posterPath()))
                .andExpect(jsonPath("$.content[0].overview").value(details.overview()))
                .andExpect(jsonPath("$.content[0].voteAverage").value(details.voteAverage()))
                .andExpect(jsonPath("$.content[0].tvShowId").exists())
                .andExpect(jsonPath("$.content[0].firstAirDate").value(details.firstAirDate()))
                .andExpect(jsonPath("$.content[0].genres").exists())
                .andExpect(jsonPath("$.content[0].productionCountries").exists())
                .andExpect(jsonPath("$.content[0].seasons").exists())
                .andExpect(jsonPath("$.content[0].overview").value(details.overview()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()));
    }

    @Test
    void getUsersWatchlistTvShows_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Long userId = 99L;

        mockMvc.perform(get("/watchlist-tv-shows/{userId}/user", userId)
                        .param("language", "en")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User " + userId + " does not exist"));
    }

    @Test
    void getMyWatchlistTvShows_shouldReturnWatchlist_whenUserIsAuthenticated() throws Exception {
        watchlistTvShowRepository.save(WatchlistTvShowEntity.builder()
                .tvShowId(100500123L)
                .currentSeason(1)
                .currentEpisode(1)
                .userId(user.getId())
                .build());

        TvShowDetailsResponse details = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Marvels")
                .posterPath("/poster.jpg")
                .overview("Overview")
                .voteAverage(9.0)
                .firstAirDate("2026-09-01")
                .numberOfEpisodes(1)
                .numberOfSeasons(1)
                .genres(List.of(new GenreResponse(1L, "Action")))
                .productionCountries(List.of(new ProductionCountriesResponse("1314FSf", "United States")))
                .seasons(List.of(new SeasonResponse(1L, "First season", 1, 1, "2026-09-01")))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(100500123L)))
                .thenReturn(Mono.just(details));

        mockMvc.perform(get("/watchlist-tv-shows/me")
                        .with(authenticatedUser(EMAIL))
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value(details.name()))
                .andExpect(jsonPath("$.content[0].posterUrl").value(details.posterPath()))
                .andExpect(jsonPath("$.content[0].overview").value(details.overview()))
                .andExpect(jsonPath("$.content[0].voteAverage").value(details.voteAverage()))
                .andExpect(jsonPath("$.content[0].tvShowId").exists())
                .andExpect(jsonPath("$.content[0].firstAirDate").value(details.firstAirDate()))
                .andExpect(jsonPath("$.content[0].genres").exists())
                .andExpect(jsonPath("$.content[0].productionCountries").exists())
                .andExpect(jsonPath("$.content[0].seasons").exists())
                .andExpect(jsonPath("$.content[0].overview").value(details.overview()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()));
    }

    @Test
    void getMyWatchlistTvShows_shouldReturnNotFound_whenUserUnauthorized() throws Exception {
        mockMvc.perform(get("/watchlist-tv-shows/me")
                        .param("language", "en"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteTvShowFromWatchlist_shouldReturnNoContent_whenTvShowIsExist() throws Exception {
        watchlistTvShowRepository.save(WatchlistTvShowEntity.builder()
                .tvShowId(100500123L)
                .currentSeason(1)
                .currentEpisode(1)
                .userId(user.getId())
                .build());

        mockMvc.perform(delete("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .param("tvShowId", "100500123"))
                .andExpect(status().isNoContent());

        assertThat(watchlistTvShowRepository.findAll()).isEmpty();
    }

    @Test
    void deleteTvShowFromWatchlist_shouldReturnNotFound_whenTvShowDoesNotExist() throws Exception {
        mockMvc.perform(delete("/watchlist-tv-shows")
                        .with(authenticatedUser(EMAIL))
                        .param("tvShowId", "100500123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tv show not found in your watchlist"));

        assertThat(watchlistTvShowRepository.findAll()).isEmpty();
    }

    @Test
    void deleteTvShowFromWatchlist_shouldReturnNotFound_whenUserUnauthorized() throws Exception {
        watchlistTvShowRepository.save(WatchlistTvShowEntity.builder()
                .tvShowId(100500123L)
                .currentSeason(1)
                .currentEpisode(1)
                .userId(user.getId())
                .build());

        mockMvc.perform(delete("/watchlist-tv-shows")
                        .param("tvShowId", "100500123"))
                .andExpect(status().isUnauthorized());

        assertThat(watchlistTvShowRepository.findAll()).isNotEmpty();
    }

    private static RequestPostProcessor authenticatedUser(String email) {
        return oidcLogin().idToken(token -> token.claim("email", email));
    }
}
