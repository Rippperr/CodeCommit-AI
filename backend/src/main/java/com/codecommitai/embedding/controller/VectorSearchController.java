package com.codecommitai.embedding.controller;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.service.VectorSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class VectorSearchController {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 50;

    private final VectorSearchService vectorSearchService;
    private final EmbeddingProvider embeddingProvider;

    public VectorSearchController(
            VectorSearchService vectorSearchService,
            EmbeddingProvider embeddingProvider
    ) {
        this.vectorSearchService =
                vectorSearchService;

        this.embeddingProvider =
                embeddingProvider;
    }

    @GetMapping
    public ResponseEntity<List<VectorSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {

        if (query == null || query.isBlank()) {

            throw new IllegalArgumentException(
                    "Query cannot be blank"
            );
        }

        if (limit <= 0) {

            throw new IllegalArgumentException(
                    "Limit must be greater than zero"
            );
        }

        if (limit > MAX_LIMIT) {

            throw new IllegalArgumentException(
                    "Limit cannot be greater than "
                            + MAX_LIMIT
            );
        }

        float[] queryVector =
                embeddingProvider.generateEmbedding(query);

        List<VectorSearchResult> results =
                vectorSearchService.search(
                        queryVector,
                        limit
                );

        return ResponseEntity.ok(results);
    }
}