package com.codecommitai.search.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KeywordSearchRepositoryTest {

    private static final UUID REPOSITORY_ID =
            UUID.fromString(
                    "a2d08518-be93-4ba0-8931-76d50eef3fd3"
            );

    @Autowired
    private KeywordSearchRepository keywordSearchRepository;

    @Test
    void search_shouldReturnMatchingChunks() {

        var results =
                keywordSearchRepository.search(
                        REPOSITORY_ID,
                        "user authentication service",
                        10
                );

        assertNotNull(results);

        assertFalse(
                results.isEmpty(),
                "Keyword search should return matching chunks"
        );

        assertTrue(
                results.size() <= 10,
                "Result count must not exceed the requested limit"
        );

        for (KeywordSearchProjection result : results) {

            assertNotNull(result.getFilePath());
            assertNotNull(result.getFileName());
            assertNotNull(result.getChunkIndex());
            assertNotNull(result.getStartLine());
            assertNotNull(result.getEndLine());
            assertNotNull(result.getContent());
            assertNotNull(result.getRelevanceScore());
        }
    }

    @Test
    void search_shouldRankHigherTokenMatchesFirst() {

        List<KeywordSearchProjection> results =
                keywordSearchRepository.search(
                        REPOSITORY_ID,
                        "user authentication service",
                        10
                );

        assertFalse(
                results.isEmpty(),
                "Search should return results"
        );

        for (int i = 0; i < results.size() - 1; i++) {

            double currentScore =
                    results.get(i)
                            .getRelevanceScore();

            double nextScore =
                    results.get(i + 1)
                            .getRelevanceScore();

            assertTrue(
                    currentScore >= nextScore,
                    "Results should be ordered by relevance score"
            );
        }
    }

    @Test
    void search_shouldGiveHigherScoreToMoreMatchingTokens() {

        List<KeywordSearchProjection> results =
                keywordSearchRepository.search(
                        REPOSITORY_ID,
                        "user authentication service",
                        10
                );

        assertFalse(
                results.isEmpty(),
                "Search should return results"
        );

        double highestScore =
                results.get(0)
                        .getRelevanceScore();

        assertEquals(
                0.95,
                highestScore,
                0.0001,
                "A result matching all three tokens should receive the highest score"
        );

        boolean containsLowerScore =
                results.stream()
                        .anyMatch(result ->
                                result.getRelevanceScore()
                                        < highestScore
                        );

        assertTrue(
                containsLowerScore,
                "Results should contain scores lower than the highest score"
        );
    }
}