package com.codecommitai.search.controller;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.HybridSearchService;
import com.codecommitai.search.service.KeywordSearchService;
import com.codecommitai.search.service.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private KeywordSearchService keywordSearchService;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private HybridSearchService hybridSearchService;

    private SearchController searchController;

    @BeforeEach
    void setUp() {
        searchController =
                new SearchController(
                        keywordSearchService,
                        semanticSearchService,
                        hybridSearchService
                );
    }

    @Test
    void search_shouldUseKeywordSearchByDefault() {

        UUID repositoryId =
                UUID.randomUUID();

        SearchResult result =
                new SearchResult(
                        "src/main/java/UserService.java",
                        "UserService.java",
                        0,
                        1,
                        20,
                        "public class UserService {}",
                        0.95,
                        0.95,
                        null
                );

        when(keywordSearchService.search(
                repositoryId,
                "UserService",
                10
        )).thenReturn(List.of(result));

        ResponseEntity<?> response =
                searchController.search(
                        repositoryId,
                        "UserService",
                        "keyword",
                        10
                );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                List.of(result),
                response.getBody()
        );

        verify(keywordSearchService)
                .search(
                        repositoryId,
                        "UserService",
                        10
                );

        verifyNoInteractions(
                semanticSearchService,
                hybridSearchService
        );
    }

    @Test
    void search_shouldUseSemanticSearchWhenTypeIsSemantic() {

        UUID repositoryId =
                UUID.randomUUID();

        VectorSearchResult result =
                new VectorSearchResult(
                        "src/main/java/UserService.java",
                        "UserService.java",
                        0,
                        1,
                        20,
                        "public class UserService {}",
                        0.15
                );

        when(semanticSearchService.search(
                repositoryId,
                "user authentication service",
                10
        )).thenReturn(List.of(result));

        ResponseEntity<?> response =
                searchController.search(
                        repositoryId,
                        "user authentication service",
                        "semantic",
                        10
                );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                List.of(result),
                response.getBody()
        );

        verify(semanticSearchService)
                .search(
                        repositoryId,
                        "user authentication service",
                        10
                );

        verifyNoInteractions(
                keywordSearchService,
                hybridSearchService
        );
    }

    @Test
    void search_shouldUseHybridSearchWhenTypeIsHybrid() {

        UUID repositoryId =
                UUID.randomUUID();

        SearchResult result =
                new SearchResult(
                        "src/main/java/AuthService.java",
                        "AuthService.java",
                        0,
                        1,
                        25,
                        "public class AuthService {}",
                        0.87,
                        0.90,
                        0.82
                );

        when(hybridSearchService.search(
                repositoryId,
                "authentication service",
                10
        )).thenReturn(List.of(result));

        ResponseEntity<?> response =
                searchController.search(
                        repositoryId,
                        "authentication service",
                        "hybrid",
                        10
                );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                List.of(result),
                response.getBody()
        );

        verify(hybridSearchService)
                .search(
                        repositoryId,
                        "authentication service",
                        10
                );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService
        );
    }

    @Test
    void search_shouldAcceptCaseInsensitiveSearchType() {

        UUID repositoryId =
                UUID.randomUUID();

        when(hybridSearchService.search(
                repositoryId,
                "authentication",
                5
        )).thenReturn(List.of());

        ResponseEntity<?> response =
                searchController.search(
                        repositoryId,
                        "authentication",
                        "HyBrId",
                        5
                );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                List.of(),
                response.getBody()
        );

        verify(hybridSearchService)
                .search(
                        repositoryId,
                        "authentication",
                        5
                );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService
        );
    }

    @Test
    void search_shouldRejectUnsupportedSearchType() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> searchController.search(
                                repositoryId,
                                "authentication",
                                "unknown",
                                10
                        )
                );

        assertEquals(
                "Unsupported search type: unknown",
                exception.getMessage()
        );

        verifyNoInteractions(
                keywordSearchService,
                semanticSearchService,
                hybridSearchService
        );
    }
}