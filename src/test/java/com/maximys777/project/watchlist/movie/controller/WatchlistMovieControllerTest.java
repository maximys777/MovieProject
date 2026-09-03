package com.maximys777.project.watchlist.movie.controller;

import com.maximys777.project.AbstractIntegrationTest;
import com.maximys777.project.exceptions.exceptions.ServiceUnavailable;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.tmdb.dto.response.movie.MovieDetails;
import com.maximys777.project.tmdb.dto.response.movie.other.GenreResponse;
import com.maximys777.project.tmdb.dto.response.movie.other.ProductionCountryResponse;
import com.maximys777.project.watchlist.movie.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import com.maximys777.project.watchlist.movie.repository.WatchlistMovieRepository;
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

public class WatchlistMovieControllerTest extends AbstractIntegrationTest {
    private static final String EMAIL = "test@gmail.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WatchlistMovieRepository watchlistMovieRepository;

    private UserEntity user;

    @BeforeEach
    void setUpUser() {
        user = userRepository.save(UserEntity.builder()
                .email(EMAIL)
                .googleId("google-123")
                .build());
    }

    @Test
    void addMovieToWatchlist_shouldCreateEntry_whenMovieNotInWatchlistYet() throws Exception {
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .movieId(1005213L)
                .title("Movie")
                .build();

        mockMvc.perform(post("/watchlist-movies")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.movieId").value(1005213L))
                .andExpect(jsonPath("$.userId").value(user.getId()));

        List<WatchlistMovieEntity> saved = watchlistMovieRepository.findAll();

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getMovieId()).isEqualTo(1005213L);
        assertThat(saved.getFirst().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void addMovieToWatchlist_shouldReturnConflict_whenMovieAlreadyInWatchlist() throws Exception {

        watchlistMovieRepository.save(WatchlistMovieEntity.builder()
                .movieId(1005213L)
                .userId(user.getId())
                .build());

        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .movieId(1005213L)
                .title("Movie")
                .build();

        mockMvc.perform(post("/watchlist-movies")
                        .with(authenticatedUser(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Movie with id 1005213 already exists in your watchlist"));

        assertThat(watchlistMovieRepository.count()).isEqualTo(1);
    }

    @Test
    void addMovieToWatchlist_shouldReturnUnauthorized_whenUserNotAuthenticated() throws Exception {
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .movieId(1005213L)
                .title("Movie")
                .build();

        mockMvc.perform(post("/watchlist-movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(watchlistMovieRepository.count()).isZero();
    }

    @Test
    void addMovieToWatchlist_shouldReturnNotFound_whenUserNotFoundInDatabase() throws Exception {
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .movieId(1005213L)
                .title("Movie")
                .build();

        mockMvc.perform(post("/watchlist-movies")
                        .with(authenticatedUser("random@gmail.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User not found"));

        assertThat(watchlistMovieRepository.count()).isZero();
    }

    @Test
    void getWatchlistMovie_shouldReturnPage_whenUsersWatchlistFound() throws Exception {
        watchlistMovieRepository.save(WatchlistMovieEntity.builder()
                .movieId(1005213L)
                .userId(user.getId())
                .build());

        MovieDetails details = new MovieDetails(
                List.of(new GenreResponse(28L, "Action")),
                "Overview",
                "/poster.jpg",
                List.of(new ProductionCountryResponse("US", "United States")),
                "2024-03-17",
                120,
                "Title",
                6.0
        );

        Mockito.when(tmdbService.getMovieDetails(Mockito.eq(1005213L), Mockito.any()))
                .thenReturn(Mono.just(details));

        mockMvc.perform(get("/watchlist-movies/{userId}/user", user.getId())
                        .with(authenticatedUser(EMAIL))
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].movieId").value(1005213L))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Title"))
                .andExpect(jsonPath("$.content[0].overview").value("Overview"))
                .andExpect(jsonPath("$.content[0].runtime").value(120))
                .andExpect(jsonPath("$.content[0].genres[0]").value("Action"));
    }

    @Test
    void getWatchlistMovie_shouldReturnNotFound_whenUserNotFoundInDatabase() throws Exception {
        mockMvc.perform(get("/watchlist-movies/{userId}/user?", 999L)
                        .with(authenticatedUser("testuser2@gmail.com"))
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("User not found"));
    }

    @Test
    void getWatchlistMovie_shouldReturnPage_whenUserNotAuthenticated() throws Exception {
        watchlistMovieRepository.save(WatchlistMovieEntity.builder()
                .movieId(1005213L)
                .userId(user.getId())
                .build());

        MovieDetails details = new MovieDetails(
                List.of(new GenreResponse(28L, "Action")),
                "Overview",
                "/poster.jpg",
                List.of(new ProductionCountryResponse("US", "United States")),
                "2024-03-17",
                120,
                "Title",
                6.0
        );

        Mockito.when(tmdbService.getMovieDetails(Mockito.eq(1005213L), Mockito.any()))
                .thenReturn(Mono.just(details));

        mockMvc.perform(get("/watchlist-movies/{userId}/user?", user.getId())
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].movieId").value(1005213L))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Title"))
                .andExpect(jsonPath("$.content[0].overview").value("Overview"))
                .andExpect(jsonPath("$.content[0].runtime").value(120))
                .andExpect(jsonPath("$.content[0].genres[0]").value("Action"));
    }

    @Test
    void getMyWatchlistMovie_shouldReturnPage_whenUserAuthenticated() throws Exception {
        watchlistMovieRepository.save(WatchlistMovieEntity.builder()
                .movieId(1005213L)
                .userId(user.getId())
                .build());

        MovieDetails details = new MovieDetails(
                List.of(new GenreResponse(28L, "Action")),
                "Overview",
                "/poster.jpg",
                List.of(new ProductionCountryResponse("US", "United States")),
                "2024-03-17",
                120,
                "Title",
                6.0
        );

        Mockito.when(tmdbService.getMovieDetails(Mockito.eq(1005213L), Mockito.any()))
                .thenReturn(Mono.just(details));

        mockMvc.perform(get("/watchlist-movies/me")
                        .param("language", "en")
                        .with(authenticatedUser(EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].movieId").value(1005213L))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Title"))
                .andExpect(jsonPath("$.content[0].overview").value("Overview"))
                .andExpect(jsonPath("$.content[0].runtime").value(120))
                .andExpect(jsonPath("$.content[0].genres[0]").value("Action"));
    }

    @Test
    void getMyWatchlistMovie_shouldReturnUnauthenticated_whenUserNotAuthenticated() throws Exception {
        mockMvc.perform(get("/watchlist-movies/me")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyWatchlistMovie_shouldReturnWatchlistMovieMapper_whenTMDBFails() throws Exception {
        watchlistMovieRepository.save(WatchlistMovieEntity.builder()
                .movieId(1005213L)
                .userId(user.getId())
                .build());

        Mockito.when(tmdbService.getMovieDetails(Mockito.eq(1005213L), Mockito.any()))
                .thenReturn(Mono.error(new ServiceUnavailable("TMDB is down")));

        mockMvc.perform(get("/watchlist-movies/me")
                        .param("language", "en")
                        .with(authenticatedUser(EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].movieId").value(1005213L))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].title").doesNotExist())
                .andExpect(jsonPath("$.content[0].overview").doesNotExist())
                .andExpect(jsonPath("$.content[0].posterUrl").doesNotExist())
                .andExpect(jsonPath("$.content[0].genres").doesNotExist());
    }

    @Test
    void deleteMovieFromWatchlist_shouldDeleteMovie_whenMovieInWatchlist() throws Exception {
        watchlistMovieRepository.save(WatchlistMovieEntity.builder()
                .movieId(1005213L)
                .userId(user.getId())
                .build());

        mockMvc.perform(delete("/watchlist-movies")
                        .with(authenticatedUser(EMAIL))
                        .param("movieId", "1005213"))
                .andExpect(status().isNoContent());

        assertThat(watchlistMovieRepository.count()).isZero();
    }

    @Test
    void deleteMovieFromWatchlist_shouldReturnNotFound_whenMovieNotFoundInWatchlist() throws Exception {
        mockMvc.perform(delete("/watchlist-movies")
                        .with(authenticatedUser(EMAIL))
                        .param("movieId", "1005213"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Movie not found in your watchlist"));
    }

    @Test
    void deleteMovieFromWatchlist_shouldReturnUnauthorized_whenUserUnauthorized() throws Exception {
        mockMvc.perform(delete("/watchlist-movies")
                        .param("movieId", "1005213"))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor authenticatedUser(String email) {
        return oidcLogin().idToken(token -> token.claim("email", email));
    }
}
