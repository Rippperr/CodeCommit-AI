package com.codecommitai.search.controller;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.KeywordSearchService;
import com.codecommitai.search.service.SemanticSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/search")
public class SearchController {

    private final KeywordSearchService keywordSearchService;
    private final SemanticSearchService semanticSearchService;

    public SearchController(
            KeywordSearchService keywordSearchService,
            SemanticSearchService semanticSearchService
    ) {
        this.keywordSearchService = keywordSearchService;
        this.semanticSearchService = semanticSearchService;
    }

    @PostMapping
    public ResponseEntity<?> search(
            @PathVariable UUID repositoryId,
            @RequestParam String query,
            @RequestParam(defaultValue = "keyword") String type,
            @RequestParam(defaultValue = "10") int limit
    ) {

        if ("keyword".equalsIgnoreCase(type)) {

            List<SearchResult> results =
                    keywordSearchService.search(
                            repositoryId,
                            query,
                            limit
                    );

            return ResponseEntity.ok(results);
        }

        if ("semantic".equalsIgnoreCase(type)) {

            List<VectorSearchResult> results =
                    semanticSearchService.search(
                            repositoryId,
                            query,
                            limit
                    );

            return ResponseEntity.ok(results);
        }

        throw new IllegalArgumentException(
                "Unsupported search type: " + type
        );
    }
}