package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchResponse;
import com.maximys777.project.tmdb.dto.response.search.SearchDropdownResponse;
import com.maximys777.project.tmdb.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "Search", description = "Endpoints for searching movies and TV shows")
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "Live search", description = "Returns dropdown search results for movies and TV shows as user types")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful search",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SearchDropdownResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)",
                    content = @Content)
    })
    @GetMapping("/live")
    public Mono<List<SearchDropdownResponse>> liveSearch(@RequestParam String query,
                                                         @RequestParam(defaultValue = "en", required = false) LanguageType language) {
        return searchService.search(query, language);
    }

    @Operation(summary = "Full search", description = "Returns paginated search results for movies and TV shows")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful search",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MultiSearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Service unavailable (TMDB service down)",
                    content = @Content)
    })
    @GetMapping("/full")
    public Mono<MultiSearchResponse> fullSearch(@RequestParam String query,
                                                @RequestParam(defaultValue = "en", required = false) LanguageType language,
                                                @RequestParam(defaultValue = "1") int page) {
        return searchService.fullSearch(query, language, page);
    }
}