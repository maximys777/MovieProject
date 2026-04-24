package com.maximys777.project.watchlist.controller.movie;

import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.tmdb.dto.response.movie.MovieDetails;
import com.maximys777.project.tmdb.service.TMDBService;
import com.maximys777.project.watchlist.movie.dto.request.AddToWatchlistMovieRequest;
import com.maximys777.project.watchlist.movie.entity.WatchlistMovieEntity;
import com.maximys777.project.watchlist.movie.repository.WatchlistMovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class WatchlistMovieControllerTest {

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TMDBService tmdbService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WatchlistMovieRepository watchlistMovieRepository;

    @Autowired
    private UserRepository userRepository;

    private WatchlistMovieEntity watchlistMovieEntity;
    private UserEntity userEntity;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @BeforeEach
    void setUp() {
        watchlistMovieRepository.deleteAll();
        userRepository.deleteAll();

        userEntity = UserEntity.builder()
                .email("test@gmail.com")
                .googleId("100003400T")
                .profilePictureUrl("http://localhost:8080/user/image")
                .build();

        userRepository.save(userEntity);

        watchlistMovieEntity = WatchlistMovieEntity.builder()
                .posterUrl("http://localhost:8080/image/poster")
                .title("Movie")
                .movieId(1005213L)
                .releaseDate(LocalDateTime.of(2024, 3, 17, 10, 5))
                .popularity(BigDecimal.valueOf(137.8))
                .userId(userEntity.getId())
                .build();

        watchlistMovieRepository.save(watchlistMovieEntity);
    }

    @Test
    void addMovieToWatchlist_ShouldReturnCreated_WhenSuccess() throws Exception {
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .posterPath("http://localhost:8080/movie/image")
                .title("New movie")
                .movieId(1005555L)
                .releaseDate(LocalDateTime.of(2020, 5, 12, 10, 5))
                .popularity(BigDecimal.valueOf(137.8))
                .build();

        mockMvc.perform(post("/watchlist-movies")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                                .idToken(token -> token.claim("email", "test@gmail.com")))
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.movieId").exists())
                .andExpect(jsonPath("$.userId").exists());

        assertThat(watchlistMovieRepository.count()).isEqualTo(2);
    }

    @Test
    void addMovieToWatchlist_ShouldThrowAlreadyExistsException_WhenMovieAlreadyInWatchlist(){
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .posterPath("http://localhost:8080/movie/image")
                .title("Movie")
                .movieId(1005213L)
                .releaseDate(LocalDateTime.of(2024, 3, 17, 10, 5))
                .popularity(BigDecimal.valueOf(137.8))
                .build();

        Exception exception = assertThrows(Exception.class, () ->
            mockMvc.perform(post("/watchlist-movies")
                    .with(oidcLogin()
                            .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                            .idToken(token -> token.claim("email", "test@gmail.com")))
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
        );

        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Movie with id 1005213 already exists in your watchlist");

        assertThat(watchlistMovieRepository.count()).isEqualTo(1);
    }

    @Test
    void addMovieToWatchlist_ShouldThrowUserNotFoundException_WhenUserNotAuthenticated() throws Exception {
        AddToWatchlistMovieRequest request = AddToWatchlistMovieRequest.builder()
                .posterPath("http://localhost:8080/movie/image")
                .title("Movie")
                .movieId(1005213L)
                .releaseDate(LocalDateTime.of(2024, 3, 17, 10, 5))
                .popularity(BigDecimal.valueOf(137.8))
                .build();

        mockMvc.perform(post("/watchlist-movies")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getWatchlistMovie_ShouldReturnUsersInformation_WhenSuccess() throws Exception {
        MovieDetails fakeTmdbDetails = new MovieDetails(
                List.of(), "Fake Overview", "/fake_poster.jpg", List.of(),
                "2024-03-17", 120, "Fake Movie", 8.5
        );

        Mockito.when(tmdbService.getMovieDetails(Mockito.anyLong(), Mockito.any()))
                .thenReturn(reactor.core.publisher.Mono.just(fakeTmdbDetails));

        mockMvc.perform(get("/watchlist-movies/{userId}/user", userEntity.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].posterUrl").value("/fake_poster.jpg"))
                .andExpect(jsonPath("$.content[0].title").value("Fake Movie"));
    }

    @Test
    void getWatchlistMovie_ShouldThrowUserNotFoundException_WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/watchlist-movies/{userId}/user", 9999L)
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getMyWatchlistMovie_ShouldReturnUserOwnWatchlist_WhenSuccess() throws Exception {

        MovieDetails fakeTmdbDetails = new MovieDetails(
                List.of(), "Fake Overview", "/fake_poster.jpg", List.of(),
                "2024-03-17", 120, "Movie", 8.5
        );

        Mockito.when(tmdbService.getMovieDetails(Mockito.anyLong(), Mockito.any()))
                .thenReturn(reactor.core.publisher.Mono.just(fakeTmdbDetails));

        mockMvc.perform(get("/watchlist-movies/me")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                                .idToken(token -> token.claim("email", "test@gmail.com"))
                        )
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].posterUrl").exists())
                .andExpect(jsonPath("$.content[0].title").value("Movie"))
                .andExpect(jsonPath("$.content[0].overview").exists())
                .andExpect(jsonPath("$.content[0].runtime").exists())
                .andExpect(jsonPath("$.content[0].releaseDate").exists())
                .andExpect(jsonPath("$.content[0].voteAverage").exists())
                .andExpect(jsonPath("$.content[0].userId").value(userEntity.getId()))
                .andExpect(jsonPath("$.content[0].genres").exists())
                .andExpect(jsonPath("$.content[0].productionCountries").exists());
    }

    @Test
    void getMyWatchlistMovie_ShouldThrowUserNotFoundException_WhenUserNotAuthenticated() throws Exception {
        mockMvc.perform(post("/watchlist-movies/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldDeleteMovieFromWatchlist_WhenSuccess() throws Exception {
        mockMvc.perform(delete("/watchlist-movies")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                                .idToken(token -> token.claim("email", "test@gmail.com")))
                        .param("movieId", String.valueOf(watchlistMovieEntity.getMovieId())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldThrowUserNotFoundException_WhenUserNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/watchlist-movies")
                        .param("movieId", String.valueOf(watchlistMovieEntity.getMovieId())))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void deleteMovieFromWatchlist_ShouldThrowNotFound_WhenMovieNotFound() throws Exception {
        mockMvc.perform(post("/watchlist-movies")
                .with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                        .idToken(token -> token.claim("email", "test@gmail.com")))
                .param("movieId", "999999")
                .contentType(MediaType.APPLICATION_JSON));

        assertThat(watchlistMovieRepository.count()).isEqualTo(1);
    }
}
