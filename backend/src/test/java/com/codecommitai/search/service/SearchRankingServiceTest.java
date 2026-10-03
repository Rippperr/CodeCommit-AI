package com.codecommitai.search.service;

import com.codecommitai.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchRankingServiceTest {

    private SearchRankingService searchRankingService;

    @BeforeEach
    void setUp() {
        searchRankingService =
                new SearchRankingService();
    }

    @Test
    void rank_shouldSortResultsByFinalScoreDescending() {

        SearchResult lowScore =
                result(
                        "src/Low.java",
                        0.20,
                        0.20,
                        "unrelated content"
                );

        SearchResult highScore =
                result(
                        "src/High.java",
                        0.90,
                        0.90,
                        "unrelated content"
                );

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(
                                lowScore,
                                highScore
                        ),
                        "authentication"
                );

        assertEquals(2, results.size());

        assertEquals(
                "src/High.java",
                results.get(0).filePath()
        );

        assertEquals(
                "src/Low.java",
                results.get(1).filePath()
        );

        assertTrue(
                results.get(0).score()
                        > results.get(1).score()
        );
    }

    @Test
    void rank_shouldApplyExactContentMatchBonus() {

        SearchResult exactMatch =
                result(
                        "src/AuthService.java",
                        0.50,
                        0.50,
                        "authentication service"
                );

        SearchResult noMatch =
                result(
                        "src/UserService.java",
                        0.50,
                        0.50,
                        "user service"
                );

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(
                                noMatch,
                                exactMatch
                        ),
                        "authentication"
                );

        assertEquals(
                "src/AuthService.java",
                results.get(0).filePath()
        );

        assertTrue(
                results.get(0).score()
                        > results.get(1).score()
        );
    }

    @Test
    void rank_shouldApplyFilePathMatchBonus() {

        SearchResult fileMatch =
                result(
                        "src/authentication/AuthService.java",
                        0.50,
                        0.50,
                        "user service"
                );

        SearchResult noFileMatch =
                result(
                        "src/user/UserService.java",
                        0.50,
                        0.50,
                        "user service"
                );

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(
                                noFileMatch,
                                fileMatch
                        ),
                        "authentication"
                );

        assertEquals(
                "src/authentication/AuthService.java",
                results.get(0).filePath()
        );

        assertTrue(
                results.get(0).score()
                        > results.get(1).score()
        );
    }

    @Test
    void rank_shouldHandleKeywordOnlyResults() {

        SearchResult result =
                result(
                        "src/UserService.java",
                        0.80,
                        null,
                        "user service"
                );

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(result),
                        "authentication"
                );

        assertEquals(1, results.size());

        assertEquals(
                0.36,
                results.get(0).score(),
                0.000001
        );

        assertEquals(
                0.80,
                results.get(0).keywordScore()
        );

        assertNull(
                results.get(0).semanticScore()
        );
    }

    @Test
    void rank_shouldHandleSemanticOnlyResults() {

        SearchResult result =
                result(
                        "src/UserService.java",
                        null,
                        0.80,
                        "user service"
                );

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(result),
                        "authentication"
                );

        assertEquals(1, results.size());

        assertEquals(
                0.32,
                results.get(0).score(),
                0.000001
        );

        assertNull(
                results.get(0).keywordScore()
        );

        assertEquals(
                0.80,
                results.get(0).semanticScore()
        );
    }

    @Test
    void rank_shouldCapScoreAtOne() {

        SearchResult result =
                result(
                        "src/authentication.java",
                        1.0,
                        1.0,
                        "authentication"
                );

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(result),
                        "authentication"
                );

        assertEquals(1.0, results.get(0).score());
    }

    @Test
    void rank_shouldReturnEmptyListForEmptyResults() {

        List<SearchResult> results =
                searchRankingService.rank(
                        List.of(),
                        "authentication"
                );

        assertTrue(results.isEmpty());
    }

    @Test
    void rank_shouldRejectBlankQuery() {

        SearchResult result =
                result(
                        "src/UserService.java",
                        0.5,
                        0.5,
                        "user service"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> searchRankingService.rank(
                                List.of(result),
                                "   "
                        )
                );

        assertEquals(
                "Search query cannot be blank",
                exception.getMessage()
        );
    }

    private SearchResult result(
            String filePath,
            Double keywordScore,
            Double semanticScore,
            String content
    ) {

        return new SearchResult(
                filePath,
                filePath.substring(
                        filePath.lastIndexOf('/') + 1
                ),
                0,
                1,
                20,
                content,
                0.0,
                keywordScore,
                semanticScore
        );
    }
}