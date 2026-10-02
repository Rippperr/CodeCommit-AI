package com.codecommitai.search.controller;

import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.KeywordSearchService;
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

    private SearchController searchController;

    @BeforeEach
    void setUp() {
        searchController = new SearchController(keywordSearchService);
    }

    @Test
    void search_shouldReturnSearchResults() {
        UUID repositoryId = UUID.randomUUID();

        SearchResult result = new SearchResult(
                "src/main/java/UserService.java",
                "UserService.java",
                0,
                10,
                25,
                "public class UserService {}",
                0.95
        );

        when(keywordSearchService.search(
                repositoryId,
                "UserService",
                10
        )).thenReturn(List.of(result));

        ResponseEntity<List<SearchResult>> response =
                searchController.search(
                        repositoryId,
                        "UserService",
                        10
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(result, response.getBody().get(0));

        verify(keywordSearchService).search(
                repositoryId,
                "UserService",
                10
        );
    }

    @Test
    void search_shouldReturnEmptyResults() {
        UUID repositoryId = UUID.randomUUID();

        when(keywordSearchService.search(
                repositoryId,
                "doesNotExist",
                10
        )).thenReturn(List.of());

        ResponseEntity<List<SearchResult>> response =
                searchController.search(
                        repositoryId,
                        "doesNotExist",
                        10
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(keywordSearchService).search(
                repositoryId,
                "doesNotExist",
                10
        );
    }

    @Test
    void search_shouldPassCustomLimit() {
        UUID repositoryId = UUID.randomUUID();

        when(keywordSearchService.search(
                repositoryId,
                "Repository",
                25
        )).thenReturn(List.of());

        ResponseEntity<List<SearchResult>> response =
                searchController.search(
                        repositoryId,
                        "Repository",
                        25
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(keywordSearchService).search(
                repositoryId,
                "Repository",
                25
        );
    }
}