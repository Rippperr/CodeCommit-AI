package com.codecommitai.search.service;

import com.codecommitai.search.dto.SearchResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SearchRankingService {

    private static final double KEYWORD_WEIGHT = 0.45;
    private static final double SEMANTIC_WEIGHT = 0.40;
    private static final double EXACT_MATCH_BONUS = 0.10;
    private static final double FILE_PATH_BONUS = 0.05;

    public List<SearchResult> rank(
            List<SearchResult> results,
            String query
    ) {

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        String normalizedQuery =
                normalizeQuery(query);

        return results.stream()
                .map(result ->
                        applyRanking(
                                result,
                                normalizedQuery
                        )
                )
                .sorted(
                        Comparator.comparing(
                                SearchResult::score,
                                Comparator.reverseOrder()
                        )
                )
                .toList();
    }

    private SearchResult applyRanking(
            SearchResult result,
            String query
    ) {

        double keywordScore =
                safeScore(result.keywordScore());

        double semanticScore =
                safeScore(result.semanticScore());

        double score =
                (keywordScore * KEYWORD_WEIGHT)
                        +
                (semanticScore * SEMANTIC_WEIGHT);

        if (hasExactMatch(result, query)) {
            score += EXACT_MATCH_BONUS;
        }

        if (hasFilePathMatch(result, query)) {
            score += FILE_PATH_BONUS;
        }

        score = Math.min(score, 1.0);

        return new SearchResult(
                result.filePath(),
                result.fileName(),
                result.chunkIndex(),
                result.startLine(),
                result.endLine(),
                result.content(),
                score,
                result.keywordScore(),
                result.semanticScore()
        );
    }

    private boolean hasExactMatch(
            SearchResult result,
            String query
    ) {

        String normalizedContent =
                result.content()
                        .toLowerCase();

        return normalizedContent.contains(
                query.toLowerCase()
        );
    }

    private boolean hasFilePathMatch(
            SearchResult result,
            String query
    ) {

        String normalizedQuery =
                query.toLowerCase();

        return result.filePath()
                .toLowerCase()
                .contains(normalizedQuery)
                ||
                result.fileName()
                        .toLowerCase()
                        .contains(normalizedQuery);
    }

    private double safeScore(Double score) {

        if (score == null) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(score, 1.0)
        );
    }

    private String normalizeQuery(String query) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query cannot be blank"
            );
        }

        return query.trim();
    }
}