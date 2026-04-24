package com.maximys777.project.watchlist.controller.tvshow;

import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import com.maximys777.project.tmdb.service.TMDBService;
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
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

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
public class WatchlistTvShowControllerTest {

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TMDBService tmdbService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WatchlistTvShowRepository watchlistTvShowRepository;

    @Autowired
    private UserRepository userRepository;

    private WatchlistTvShowEntity watchlistTvShowEntity;
    private UserEntity userEntity;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @BeforeEach
    void setUp() {
        watchlistTvShowRepository.deleteAll();
        userRepository.deleteAll();

        userEntity = UserEntity.builder()
                .email("test@gmail.com")
                .googleId("100003400T")
                .profilePictureUrl("http://localhost:8080/user/image")
                .build();

        userRepository.save(userEntity);

        watchlistTvShowEntity = WatchlistTvShowEntity.builder()
                .tvShowId(1005213L)
                .name("Tv show")
                .posterUrl("http://localhost:8080/tvshows/image")
                .firstAirDate("2024-12-05")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .userId(userEntity.getId())
                .build();

        watchlistTvShowRepository.save(watchlistTvShowEntity);
    }

    @Test
    void addToWatchlistTvShow_ShouldReturnWatchlistTvShowResponse_WhenSuccess() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(10051234L)
                .name("Kong")
                .posterUrl("http://localhost:8080/tvshows/image")
                .firstAirDate("2026-04-23")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        GenreResponse genre = GenreResponse.builder()
                .id(1L)
                .name("Fantasy")
                .build();

        ProductionCountriesResponse productionCountry = ProductionCountriesResponse.builder()
                .iso_3166_1("ss8593245t_")
                .name("United States")
                .build();

        SeasonResponse seasonResponse = SeasonResponse.builder()
                .id(1L)
                .name("Season 1")
                .seasonNumber(1)
                .episodeCount(12)
                .airDate("2026-04-23")
                .build();

        TvShowDetailsResponse tmdbResponse = TvShowDetailsResponse.builder()
                .id(10051234L)
                .name("Kong")
                .overview("Kong overview")
                .numberOfSeasons(3)
                .numberOfEpisodes(12)
                .genres(List.of(genre))
                .productionCountries(List.of(productionCountry))
                .seasons(List.of(seasonResponse))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(10051234L)))
                .thenReturn(Mono.just(tmdbResponse));

        mockMvc.perform(post("/watchlist-tv-shows")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                                .idToken(token -> token.claim("email", "test@gmail.com")))
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Kong"))
                .andExpect(jsonPath("$.tvShowId").value(10051234L))
                .andExpect(jsonPath("$.userId").value(userEntity.getId()));

        assertThat(watchlistTvShowRepository.count()).isEqualTo(2);
    }

    @Test
    void addToWatchlistTvShow_ShouldThrowUserNotFound_WhenUserNotAuthenticated() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(10051234L)
                .name("Kong")
                .posterUrl("http://localhost:8080/tvshows/image")
                .firstAirDate("2026-04-23")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        mockMvc.perform(post("/watchlist-tv-shows")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void addToWatchlistTvShow_ShouldThrowIllegalException_WhenTvShowDoesntExist() {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(10051234L)
                .name("Kong")
                .posterUrl("http://localhost:8080/tvshows/image")
                .firstAirDate("2026-04-23")
                .originCountry("United States")
                .currentSeason(1)
                .currentEpisode(1)
                .build();

        Exception exception = assertThrows(Exception.class, () ->
            mockMvc.perform(post("/watchlist-tv-shows")
                    .with(oidcLogin()
                            .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                            .idToken(token -> token.claim("email", "test@gmail.com")))
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
        );

        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);

        assertThat(watchlistTvShowRepository.count()).isEqualTo(1);
    }

    @Test
    void addToWatchlistTvShow_ShouldThrowIllegalException_WhenSeasonDoesntExist() throws Exception {
        AddToWatchlistTvShowRequest request = AddToWatchlistTvShowRequest.builder()
                .tvShowId(10051234L)
                .name("Kong")
                .posterUrl("http://localhost:8080/tvshows/image")
                .firstAirDate("2026-04-23")
                .originCountry("United States")
                .currentSeason(100)
                .currentEpisode(1)
                .build();

        GenreResponse genre = GenreResponse.builder()
                .id(1L)
                .name("Fantasy")
                .build();

        ProductionCountriesResponse productionCountry = ProductionCountriesResponse.builder()
                .iso_3166_1("ss8593245t_")
                .name("United States")
                .build();

        SeasonResponse seasonResponse = SeasonResponse.builder()
                .id(1L)
                .name("Season 1")
                .seasonNumber(1)
                .episodeCount(12)
                .airDate("2026-04-23")
                .build();

        TvShowDetailsResponse tmdbResponse = TvShowDetailsResponse.builder()
                .id(10051234L)
                .name("Kong")
                .overview("Kong overview")
                .numberOfSeasons(3)
                .numberOfEpisodes(12)
                .genres(List.of(genre))
                .productionCountries(List.of(productionCountry))
                .seasons(List.of(seasonResponse))
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.eq(10051234L)))
                .thenReturn(Mono.just(tmdbResponse));

        Exception exception = assertThrows(Exception.class, () ->
            mockMvc.perform(post("/watchlist-tv-shows")
                    .with(oidcLogin()
                            .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                            .idToken(token -> token.claim("email", "test@gmail.com")))
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
        );

        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Season 100 does not exist");

        assertThat(watchlistTvShowRepository.count()).isEqualTo(1);
    }

    @Test
    void getUsersWatchlistTvShows_ShouldReturnWatchlistTvShowResponse_WhenSuccess() throws Exception {
        TvShowDetailsResponse response = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Kong")
                .overview("Kong overview")
                .tvShowId(10051234L)
                .voteAverage(9.2)
                .genres(List.of())
                .productionCountries(List.of())
                .numberOfSeasons(1)
                .numberOfEpisodes(12)
                .seasons(List.of())
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.anyLong(), Mockito.any()))
                .thenReturn(reactor.core.publisher.Mono.just(response));

        mockMvc.perform(get("/watchlist-tv-shows/{userId}/user", userEntity.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].name").value("Kong"))
                .andExpect(jsonPath("$.content[0].overview").value("Kong overview"));

    }

    @Test
    void getUsersWatchlistTvShows_ShouldThrowNotFound_WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/watchlist-tv-shows/{userId}/user", 999L)
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getMyWatchlistTvShows_ShouldReturnWatchlistTvShowResponse_WhenSuccess() throws Exception {
        TvShowDetailsResponse response = TvShowDetailsResponse.builder()
                .id(1L)
                .name("Kong")
                .overview("Kong overview")
                .tvShowId(10051234L)
                .voteAverage(9.2)
                .genres(List.of())
                .productionCountries(List.of())
                .numberOfSeasons(1)
                .numberOfEpisodes(12)
                .seasons(List.of())
                .build();

        Mockito.when(tmdbService.getTvShowDetails(Mockito.anyLong(), Mockito.any()))
                .thenReturn(reactor.core.publisher.Mono.just(response));

        mockMvc.perform(get("/watchlist-tv-shows/me")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                                .idToken(token -> token.claim("email", "test@gmail.com")))
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].name").value("Kong"))
                .andExpect(jsonPath("$.content[0].overview").value("Kong overview"));
    }

    @Test
    void getMyWatchlistTvShows_ShouldThrowUnauthorized_WhenUserNotAuthorized() throws Exception {
        mockMvc.perform(get("/watchlist-tv-shows/me")
                        .param("page", "0")
                        .param("size", "10")
                        .param("language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void deleteTvShowFromWatchlist_ShouldDeleteTvShowFromWatchlist_WhenSuccess() throws Exception {
        mockMvc.perform(delete("/watchlist-tv-shows")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                                .idToken(token -> token.claim("email", "test@gmail.com")))
                        .param("tvShowId", watchlistTvShowEntity.getTvShowId().toString()))
                .andExpect(status().isOk());

        assertThat(watchlistTvShowRepository.count()).isZero();
    }

    @Test
    void deleteTvShowFromWatchlist_ShouldThrowUnauthorized_WhenUserNotAuthorized() throws Exception {
        mockMvc.perform(delete("/watchlist-tv-shows")
                        .param("tvShowId", watchlistTvShowEntity.getTvShowId().toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void deleteTvShowFromWatchlist_ShouldThrowNotFound_WhenTvShowNotFoundInWatchlist() {
        Exception exception = assertThrows(Exception.class, () ->
            mockMvc.perform(delete("/watchlist-tv-shows")
                    .with(oidcLogin()
                            .authorities(new SimpleGrantedAuthority("SCOPE_profile"))
                            .idToken(token -> token.claim("email", "test@gmail.com")))
                    .param("tvShowId", "999"))
        );

        assertThat(exception.getCause()).isNotNull();
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Tv show not found in your watchlist");

        assertThat(watchlistTvShowRepository.count()).isEqualTo(1);
    }
}
