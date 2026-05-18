package com.maximys777.project.tmdb.controller;

import com.maximys777.project.tmdb.common.LanguageType;
import com.maximys777.project.tmdb.dto.response.search.MultiSearchResponse;
import com.maximys777.project.tmdb.dto.response.search.SearchDropdownResponse;
import com.maximys777.project.tmdb.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/live")
    public Mono<List<SearchDropdownResponse>> liveSearch(@RequestParam String query,
                                                         @RequestParam(defaultValue = "en", required = false) LanguageType language) {
        return searchService.search(query, language);
    }

    @GetMapping("/full")
    public Mono<MultiSearchResponse> fullSearch(@RequestParam String query,
                                                @RequestParam(defaultValue = "en", required = false) LanguageType language,
                                                @RequestParam(defaultValue = "1") int page) {
        return searchService.fullSearch(query, language, page);
    }
}