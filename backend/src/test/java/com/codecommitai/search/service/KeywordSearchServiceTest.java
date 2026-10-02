package com.codecommitai.search.service;

import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.repository.KeywordSearchProjection;
import com.codecommitai.search.repository.KeywordSearchRepository;
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
class KeywordSearchServiceTest {

    @Mock
    private KeywordSearchRepository keywordSearchRepository;

    @Mock
    private KeywordSearchProjection projection;

    private KeywordSearchService keywordSearchService;

    @BeforeEach
    void setUp() {
        keywordSearchService = new KeywordSearchService(keywordSearchRepository);
    }

    @Test
    void search_shouldReturnMappedResults() {
        UUID repositoryId = UUID.randomUUID();

        when(projection.getFilePath())
                .thenReturn("src/main/java/UserService.java");
        when(projection.getFileName())
                .thenReturn("UserService.java");
        when(projection.getChunkIndex())
                .thenReturn(0);
        when(projection.getStartLine())
                .thenReturn(10);
        when(projection.getEndLine())
                .thenReturn(25);
        when(projection.getContent())
                .thenReturn("public class UserService {}");
        when(projection.getRelevanceScore())
                .thenReturn(0.95);

        when(keywordSearchRepository.search(
                eq(repositoryId),
                eq("UserService"),
                eq("UserService"),
                eq(10)
        )).thenReturn(List.of(projection));

        List<SearchResult> results =
                keywordSearchService.search(repositoryId, "UserService");

        assertEquals(1, results.size());

        SearchResult result = results.get(0);

        assertEquals(
                "src/main/java/UserService.java",
                result.filePath()
        );
        assertEquals(
                "UserService.java",
                result.fileName()
        );
        assertEquals(0, result.chunkIndex());
        assertEquals(10, result.startLine());
        assertEquals(25, result.endLine());
        assertEquals(
                "public class UserService {}",
                result.content()
        );
        assertEquals(0.95, result.relevanceScore());
    }

    @Test
    void search_shouldUseProvidedLimit() {
        UUID repositoryId = UUID.randomUUID();

        when(keywordSearchRepository.search(
                eq(repositoryId),
                eq("authentication"),
                eq("authentication"),
                eq(25)
        )).thenReturn(List.of());

        List<SearchResult> results =
                keywordSearchService.search(
                        repositoryId,
                        "authentication",
                        25
                );

        assertTrue(results.isEmpty());

        verify(keywordSearchRepository).search(
                repositoryId,
                "authentication",
                "authentication",
                25
        );
    }

    @Test
    void search_shouldTrimQuery() {
        UUID repositoryId = UUID.randomUUID();

        when(keywordSearchRepository.search(
                eq(repositoryId),
                eq("authentication"),
                eq("authentication"),
                eq(10)
        )).thenReturn(List.of());

        keywordSearchService.search(
                repositoryId,
                "   authentication   "
        );

        verify(keywordSearchRepository).search(
                repositoryId,
                "authentication",
                "authentication",
                10
        );
    }

    @Test
    void search_shouldEscapeSqlWildcards() {
        UUID repositoryId = UUID.randomUUID();

        when(keywordSearchRepository.search(
                eq(repositoryId),
                eq("100%_complete"),
                eq("100\\%\\_complete"),
                eq(10)
        )).thenReturn(List.of());

        keywordSearchService.search(
                repositoryId,
                "100%_complete"
        );

        verify(keywordSearchRepository).search(
                repositoryId,
                "100%_complete",
                "100\\%\\_complete",
                10
        );
    }

    @Test
    void search_shouldRejectNullRepositoryId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keywordSearchService.search(
                        null,
                        "authentication"
                )
        );

        assertEquals(
                "Repository ID cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(keywordSearchRepository);
    }

    @Test
    void search_shouldRejectBlankQuery() {
        UUID repositoryId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keywordSearchService.search(
                        repositoryId,
                        "   "
                )
        );

        assertEquals(
                "Search query cannot be blank",
                exception.getMessage()
        );

        verifyNoInteractions(keywordSearchRepository);
    }

    @Test
    void search_shouldRejectZeroLimit() {
        UUID repositoryId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keywordSearchService.search(
                        repositoryId,
                        "authentication",
                        0
                )
        );

        assertEquals(
                "Search limit must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(keywordSearchRepository);
    }

    @Test
    void search_shouldRejectLimitAboveMaximum() {
        UUID repositoryId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keywordSearchService.search(
                        repositoryId,
                        "authentication",
                        51
                )
        );

        assertEquals(
                "Search limit cannot exceed 50",
                exception.getMessage()
        );

        verifyNoInteractions(keywordSearchRepository);
    }

    @Test
    void search_shouldRejectQueryAboveMaximumLength() {
        UUID repositoryId = UUID.randomUUID();
        String query = "a".repeat(501);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keywordSearchService.search(
                        repositoryId,
                        query
                )
        );

        assertEquals(
                "Search query cannot exceed 500 characters",
                exception.getMessage()
        );

        verifyNoInteractions(keywordSearchRepository);
    }
}