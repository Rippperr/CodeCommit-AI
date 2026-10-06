package com.codecommitai.search.service;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private KeywordSearchService keywordSearchService;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private SearchRankingService searchRankingService;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        hybridSearchService =
                new HybridSearchService(
                        keywordSearchService,
                        semanticSearchService,
                        searchRankingService
                );
    }

    @Test
    void search_shouldCombineKeywordAndSemanticResults() {

        UUID repositoryId =
                UUID.randomUUID();

        SearchResult keywordResult =
                new SearchResult(
                        "src/UserService.java",
                        "UserService.java",
                        0,
                        1,
                        20,
                        "public class UserService {}",
                        0.8,
                        0.8,
                        null
                );

        VectorSearchResult semanticResult =
                new VectorSearchResult(
                        "src/AuthService.java",
                        "AuthService.java",
                        0,
                        1,
                        20,
                        "public class AuthService {}",
                        0.2
                );

        SearchResult rankedKeywordResult =
                new SearchResult(
                        "src/UserService.java",
                        "UserService.java",
                        0,
                        1,
                        20,
                        "public class UserService {}",
                        0.36,
                        0.8,
                        null
                );

        SearchResult rankedSemanticResult =
                new SearchResult(
                        "src/AuthService.java",
                        "AuthService.java",
                        0,
                        1,
                        20,
                        "public class AuthService {}",
                        0.32,
                        null,
                        0.8
                );

        when(keywordSearchService.search(
                repositoryId,
                "authentication",
                20
        )).thenReturn(List.of(keywordResult));

        when(semanticSearchService.search(
                repositoryId,
                "authentication",
                20
        )).thenReturn(List.of(semanticResult));

        when(searchRankingService.rank(
                anyList(),
                eq("authentication")
        )).thenReturn(
                List.of(
                        rankedKeywordResult,
                        rankedSemanticResult
                )
        );

        List<SearchResult> results =
                hybridSearchService.search(
                        repositoryId,
                        "authentication",
                        10
                );

        assertEquals(2, results.size());

        assertEquals(
                "src/UserService.java",
                results.get(0).filePath()
        );

        assertEquals(
                "src/AuthService.java",
                results.get(1).filePath()
        );

        verify(keywordSearchService)
                .search(
                        repositoryId,
                        "authentication",
                        20
                );

        verify(semanticSearchService)
                .search(
                        repositoryId,
                        "authentication",
                        20
                );

        verify(searchRankingService)
                .rank(
                        anyList(),
                        eq("authentication")
                );
    }

    @Test
    void search_shouldMergeSameFileAndChunkFromBothSources() {

        UUID repositoryId =
                UUID.randomUUID();

        SearchResult keywordResult =
                new SearchResult(
                        "src/UserService.java",
                        "UserService.java",
                        0,
                        1,
                        20,
                        "public class UserService {}",
                        0.8,
                        0.8,
                        null
                );

        VectorSearchResult semanticResult =
                new VectorSearchResult(
                        "src/UserService.java",
                        "UserService.java",
                        0,
                        1,
                        20,
                        "public class UserService {}",
                        0.2
                );

        when(keywordSearchService.search(
                repositoryId,
                "user service",
                20
        )).thenReturn(List.of(keywordResult));

        when(semanticSearchService.search(
                repositoryId,
                "user service",
                20
        )).thenReturn(List.of(semanticResult));

        when(searchRankingService.rank(
                anyList(),
                eq("user service")
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        List<SearchResult> results =
                hybridSearchService.search(
                        repositoryId,
                        "user service",
                        10
                );

        assertEquals(
                1,
                results.size(),
                "The same file/chunk should only appear once"
        );

        SearchResult result =
                results.get(0);

        assertEquals(
                0.8,
                result.keywordScore()
        );

        assertEquals(
                0.8,
                result.semanticScore(),
                0.000001
        );
    }

    @Test
    void search_shouldConvertCosineDistanceToSimilarity() {

        UUID repositoryId =
                UUID.randomUUID();

        VectorSearchResult semanticResult =
                new VectorSearchResult(
                        "src/AuthService.java",
                        "AuthService.java",
                        0,
                        1,
                        20,
                        "authentication service",
                        0.25
                );

        when(keywordSearchService.search(
                repositoryId,
                "authentication",
                20
        )).thenReturn(List.of());

        when(semanticSearchService.search(
                repositoryId,
                "authentication",
                20
        )).thenReturn(List.of(semanticResult));

        when(searchRankingService.rank(
                anyList(),
                eq("authentication")
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        List<SearchResult> results =
                hybridSearchService.search(
                        repositoryId,
                        "authentication",
                        10
                );

        assertEquals(1, results.size());

        assertEquals(
                0.75,
                results.get(0).semanticScore(),
                0.000001
        );
    }

    @Test
    void search_shouldRespectRequestedLimit() {

        UUID repositoryId =
                UUID.randomUUID();

        when(keywordSearchService.search(
                repositoryId,
                "authentication",
                6
        )).thenReturn(List.of());

        when(semanticSearchService.search(
                repositoryId,
                "authentication",
                6
        )).thenReturn(List.of());

        when(searchRankingService.rank(
                anyList(),
                eq("authentication")
        )).thenReturn(List.of());

        List<SearchResult> results =
                hybridSearchService.search(
                        repositoryId,
                        "authentication",
                        3
                );

        assertTrue(results.size() <= 3);

        verify(keywordSearchService)
                .search(
                        repositoryId,
                        "authentication",
                        6
                );

        verify(semanticSearchService)
                .search(
                        repositoryId,
                        "authentication",
                        6
                );
    }

    @Test
    void search_shouldRejectNullRepositoryId() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> hybridSearchService.search(
                                null,
                                "authentication",
                                10
                        )
                );

        assertEquals(
                "Repository ID cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService,
                searchRankingService
        );
    }

    @Test
    void search_shouldRejectBlankQuery() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> hybridSearchService.search(
                                repositoryId,
                                "   ",
                                10
                        )
                );

        assertEquals(
                "Search query cannot be blank",
                exception.getMessage()
        );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService,
                searchRankingService
        );
    }

    @Test
    void search_shouldRejectZeroLimit() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> hybridSearchService.search(
                                repositoryId,
                                "authentication",
                                0
                        )
                );

        assertEquals(
                "Search limit must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService,
                searchRankingService
        );
    }

    @Test
    void search_shouldRejectLimitAboveMaximum() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> hybridSearchService.search(
                                repositoryId,
                                "authentication",
                                51
                        )
                );

        assertEquals(
                "Search limit cannot exceed 50",
                exception.getMessage()
        );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService,
                searchRankingService
        );
    }
}