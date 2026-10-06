package com.codecommitai.search.service;

import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.embedding.dto.VectorSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HybridSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final KeywordSearchService keywordSearchService;
    private final SemanticSearchService semanticSearchService;
    private final SearchRankingService searchRankingService;

    public HybridSearchService(
            KeywordSearchService keywordSearchService,
            SemanticSearchService semanticSearchService,
            SearchRankingService searchRankingService
    ) {
        this.keywordSearchService =
                keywordSearchService;

        this.semanticSearchService =
                semanticSearchService;

        this.searchRankingService =
                searchRankingService;
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

        /*
         * Retrieve more candidates than the final requested limit.
         *
         * This gives the ranking component enough candidates from
         * both keyword and semantic retrieval.
         */
        int candidateLimit =
                Math.min(
                        normalizedLimit * 2,
                        MAX_LIMIT
                );

        List<SearchResult> keywordResults =
                keywordSearchService.search(
                        repositoryId,
                        normalizedQuery,
                        candidateLimit
                );

        List<VectorSearchResult> semanticResults =
                semanticSearchService.search(
                        repositoryId,
                        normalizedQuery,
                        candidateLimit
                );

        Map<String, SearchResult> mergedResults =
                mergeResults(
                        keywordResults,
                        semanticResults
                );

        List<SearchResult> rankedResults =
                searchRankingService.rank(
                        new ArrayList<>(
                                mergedResults.values()
                        ),
                        normalizedQuery
                );

        return rankedResults.stream()
                .limit(normalizedLimit)
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

    private Map<String, SearchResult> mergeResults(
            List<SearchResult> keywordResults,
            List<VectorSearchResult> semanticResults
    ) {

        Map<String, SearchResult> mergedResults =
                new LinkedHashMap<>();

        for (SearchResult result : keywordResults) {

            String key =
                    buildResultKey(
                            result.filePath(),
                            result.chunkIndex()
                    );

            mergedResults.put(
                    key,
                    result
            );
        }

        for (VectorSearchResult result : semanticResults) {

            String key =
                    buildResultKey(
                            result.filePath(),
                            result.chunkIndex()
                    );

            SearchResult existingResult =
                    mergedResults.get(key);

            if (existingResult == null) {

                mergedResults.put(
                        key,
                        new SearchResult(
                                result.filePath(),
                                result.fileName(),
                                result.chunkIndex(),
                                result.startLine(),
                                result.endLine(),
                                result.content(),
                                0.0,
                                null,
                                convertDistanceToSimilarity(
                                        result.cosineDistance()
                                )
                        )
                );

            } else {

                mergedResults.put(
                        key,
                        new SearchResult(
                                existingResult.filePath(),
                                existingResult.fileName(),
                                existingResult.chunkIndex(),
                                existingResult.startLine(),
                                existingResult.endLine(),
                                existingResult.content(),
                                existingResult.score(),
                                existingResult.keywordScore(),
                                convertDistanceToSimilarity(
                                        result.cosineDistance()
                                )
                        )
                );
            }
        }

        return mergedResults;
    }

    private double convertDistanceToSimilarity(
            Double cosineDistance
    ) {

        if (cosineDistance == null) {
            return 0.0;
        }

        double similarity =
                1.0 - cosineDistance;

        return Math.max(
                0.0,
                Math.min(
                        similarity,
                        1.0
                )
        );
    }

    private String buildResultKey(
            String filePath,
            Integer chunkIndex
    ) {

        return filePath
                + "::"
                + chunkIndex;
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

        return query.trim();
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