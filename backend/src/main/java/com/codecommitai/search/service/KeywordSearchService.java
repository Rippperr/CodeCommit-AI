package com.codecommitai.search.service;

import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.repository.KeywordSearchProjection;
import com.codecommitai.search.repository.KeywordSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KeywordSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY_LENGTH = 500;

    private final KeywordSearchRepository keywordSearchRepository;

    public KeywordSearchService(
            KeywordSearchRepository keywordSearchRepository
    ) {
        this.keywordSearchRepository =
                keywordSearchRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(
            UUID repositoryId,
            String query,
            int limit
    ) {
        validateRepositoryId(repositoryId);

        String normalizedQuery =
                normalizeQuery(query);

        int normalizedLimit =
                normalizeLimit(limit);

        return keywordSearchRepository
                .search(
                        repositoryId,
                        normalizedQuery,
                        normalizedLimit
                )
                .stream()
                .map(this::toSearchResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(
            UUID repositoryId,
            String query
    ) {
        return search(
                repositoryId,
                query,
                DEFAULT_LIMIT
        );
    }

    private SearchResult toSearchResult(
            KeywordSearchProjection projection
    ) {
        Double keywordScore =
                projection.getRelevanceScore();

        return new SearchResult(
                projection.getFilePath(),
                projection.getFileName(),
                projection.getChunkIndex(),
                projection.getStartLine(),
                projection.getEndLine(),
                projection.getContent(),
                keywordScore,
                keywordScore,
                null
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