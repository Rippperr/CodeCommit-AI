package com.codecommitai.search.service;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.service.VectorSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SemanticSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY_LENGTH = 500;

    private final VectorSearchService vectorSearchService;
    private final EmbeddingProvider embeddingProvider;

    public SemanticSearchService(
            VectorSearchService vectorSearchService,
            EmbeddingProvider embeddingProvider
    ) {
        this.vectorSearchService = vectorSearchService;
        this.embeddingProvider = embeddingProvider;
    }

    @Transactional(readOnly = true)
    public List<VectorSearchResult> search(
            UUID repositoryId,
            String query,
            int limit
    ) {

        validateRepositoryId(repositoryId);

        String normalizedQuery =
                normalizeQuery(query);

        int normalizedLimit =
                normalizeLimit(limit);

        float[] queryVector =
                embeddingProvider.generateEmbedding(
                        normalizedQuery
                );

        return vectorSearchService.searchByRepository(
                repositoryId,
                queryVector,
                normalizedLimit
        );
    }

    @Transactional(readOnly = true)
    public List<VectorSearchResult> search(
            UUID repositoryId,
            String query
    ) {

        return search(
                repositoryId,
                query,
                DEFAULT_LIMIT
        );
    }

    private void validateRepositoryId(
            UUID repositoryId
    ) {

        if (repositoryId == null) {

            throw new IllegalArgumentException(
                    "Repository ID cannot be null"
            );
        }
    }

    private String normalizeQuery(
            String query
    ) {

        if (query == null || query.isBlank()) {

            throw new IllegalArgumentException(
                    "Search query cannot be blank"
            );
        }

        String normalized =
                query.trim();

        if (normalized.length() > MAX_QUERY_LENGTH) {

            throw new IllegalArgumentException(
                    "Search query cannot exceed "
                            + MAX_QUERY_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    private int normalizeLimit(
            int limit
    ) {

        if (limit <= 0) {

            throw new IllegalArgumentException(
                    "Search limit must be greater than zero"
            );
        }

        if (limit > MAX_LIMIT) {

            throw new IllegalArgumentException(
                    "Search limit cannot exceed "
                            + MAX_LIMIT
            );
        }

        return limit;
    }
}
