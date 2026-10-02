package com.codecommitai.search.controller;

import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.KeywordSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/search")
public class SearchController {

    private final KeywordSearchService keywordSearchService;

    public SearchController(KeywordSearchService keywordSearchService) {
        this.keywordSearchService = keywordSearchService;
    }

    @PostMapping
    public ResponseEntity<List<SearchResult>> search(
            @PathVariable UUID repositoryId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<SearchResult> results =
                keywordSearchService.search(
                        repositoryId,
                        query,
                        limit
                );

        return ResponseEntity.ok(results);
    }
}