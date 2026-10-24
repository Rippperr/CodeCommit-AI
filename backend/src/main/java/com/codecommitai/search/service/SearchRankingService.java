
package com.codecommitai.search.service;

import com.codecommitai.search.dto.SearchResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchRankingService {

    private static final double KEYWORD_WEIGHT = 0.45;
    private static final double SEMANTIC_WEIGHT = 0.40;
    private static final double EXACT_MATCH_BONUS = 0.10;
    private static final double FILE_PATH_BONUS = 0.05;

    private static final double PRODUCTION_CODE_BONUS = 0.08;
    private static final double TEST_CODE_PENALTY = 0.08;
    private static final double NAMED_IDENTIFIER_BONUS = 0.20;

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[a-zA-Z0-9_]+");

    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile(
                    "\\b[A-Z][a-zA-Z0-9]*[A-Z][a-zA-Z0-9]*\\b"
            );

    public List<SearchResult> rank(
            List<SearchResult> results,
            String query
    ) {

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        String normalizedQuery = normalizeQuery(query);

        return results.stream()
                .map(result ->
                        applyRanking(result, normalizedQuery)
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
                        + (semanticScore * SEMANTIC_WEIGHT);

        if (hasExactMatch(result, query)) {
            score += EXACT_MATCH_BONUS;
        }

        if (hasFilePathMatch(result, query)) {
            score += FILE_PATH_BONUS;
        }

        if (hasNamedIdentifierPathMatch(result, query)) {
            score += NAMED_IDENTIFIER_BONUS;
        }

        if (isProductionFile(result.filePath())) {
            score += PRODUCTION_CODE_BONUS;
        } else if (isTestFile(result.filePath())) {
            score -= TEST_CODE_PENALTY;
        }

        score = Math.max(
                0.0,
                Math.min(score, 1.0)
        );

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

    private boolean hasNamedIdentifierPathMatch(
            SearchResult result,
            String query
    ) {

        String filePath = result.filePath();

        if (filePath == null || filePath.isBlank()) {
            return false;
        }

        String normalizedPath =
                filePath.replace('\\', '/');

        String fileName = normalizedPath.substring(
                normalizedPath.lastIndexOf('/') + 1
        );

        if (fileName.toLowerCase(Locale.ROOT).endsWith(".java")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }

        Matcher matcher = IDENTIFIER_PATTERN.matcher(query);

        while (matcher.find()) {
            String identifier = matcher.group();

            if (fileName.equalsIgnoreCase(identifier)) {
                return true;
            }
        }

        return false;
    }

    private boolean isProductionFile(String filePath) {
        if (filePath == null) {
            return false;
        }

        String normalizedPath =
                filePath.replace('\\', '/')
                        .toLowerCase(Locale.ROOT);

        return normalizedPath.contains("/src/main/");
    }

    private boolean isTestFile(String filePath) {
        if (filePath == null) {
            return false;
        }

        String normalizedPath =
                filePath.replace('\\', '/')
                        .toLowerCase(Locale.ROOT);

        return normalizedPath.contains("/src/test/")
                || normalizedPath.contains("/test/")
                || normalizedPath.endsWith("test.java")
                || normalizedPath.endsWith("tests.java");
    }

    private boolean hasExactMatch(
            SearchResult result,
            String query
    ) {

        String content = result.content();

        if (content == null || content.isBlank()) {
            return false;
        }

        String normalizedContent =
                content.toLowerCase(Locale.ROOT);

        return normalizedContent.contains(
                query.toLowerCase(Locale.ROOT)
        );
    }

    private boolean hasFilePathMatch(
            SearchResult result,
            String query
    ) {

        String filePath = result.filePath();
        String fileName = result.fileName();

        String normalizedPath =
                filePath == null
                        ? ""
                        : filePath.toLowerCase(Locale.ROOT);

        String normalizedFileName =
                fileName == null
                        ? ""
                        : fileName.toLowerCase(Locale.ROOT);

        String normalizedQuery =
                query.toLowerCase(Locale.ROOT);

        if (normalizedPath.contains(normalizedQuery)
                || normalizedFileName.contains(normalizedQuery)) {
            return true;
        }

        for (String token : extractTokens(query)) {
            if (token.length() < 4) {
                continue;
            }

            if (normalizedPath.contains(token)
                    || normalizedFileName.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private List<String> extractTokens(String query) {
        return TOKEN_PATTERN.matcher(query)
                .results()
                .map(match ->
                        match.group().toLowerCase(Locale.ROOT)
                )
                .distinct()
                .toList();
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
