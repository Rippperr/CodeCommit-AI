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
        keywordSearchService =
                new KeywordSearchService(
                        keywordSearchRepository
                );
    }

    @Test
    void search_shouldReturnMappedResults() {

        UUID repositoryId =
                UUID.randomUUID();

        when(projection.getFilePath())
                .thenReturn(
                        "src/main/java/UserService.java"
                );

        when(projection.getFileName())
                .thenReturn("UserService.java");

        when(projection.getChunkIndex())
                .thenReturn(0);

        when(projection.getStartLine())
                .thenReturn(1);

        when(projection.getEndLine())
                .thenReturn(20);

        when(projection.getContent())
                .thenReturn(
                        "public class UserService {}"
                );

        when(projection.getRelevanceScore())
                .thenReturn(0.95);

        when(keywordSearchRepository.search(
                repositoryId,
                "UserService",
                10
        )).thenReturn(List.of(projection));

        List<SearchResult> results =
                keywordSearchService.search(
                        repositoryId,
                        "UserService",
                        10
                );

        assertEquals(1, results.size());

        SearchResult result =
                results.get(0);

        assertEquals(
                "src/main/java/UserService.java",
                result.filePath()
        );

        assertEquals(
                "UserService.java",
                result.fileName()
        );

        assertEquals(
                0,
                result.chunkIndex()
        );

        assertEquals(
                1,
                result.startLine()
        );

        assertEquals(
                20,
                result.endLine()
        );

        assertEquals(
                "public class UserService {}",
                result.content()
        );

        assertEquals(
                0.95,
                result.score()
        );

        assertEquals(
                0.95,
                result.keywordScore()
        );

        assertNull(
                result.semanticScore()
        );

        verify(keywordSearchRepository)
                .search(
                        repositoryId,
                        "UserService",
                        10
                );
    }

    @Test
    void search_shouldUseDefaultLimit() {

        UUID repositoryId =
                UUID.randomUUID();

        when(keywordSearchRepository.search(
                repositoryId,
                "authentication",
                10
        )).thenReturn(List.of());

        List<SearchResult> results =
                keywordSearchService.search(
                        repositoryId,
                        "authentication"
                );

        assertTrue(results.isEmpty());

        verify(keywordSearchRepository)
                .search(
                        repositoryId,
                        "authentication",
                        10
                );
    }

    @Test
    void search_shouldTrimQuery() {

        UUID repositoryId =
                UUID.randomUUID();

        when(keywordSearchRepository.search(
                repositoryId,
                "authentication",
                10
        )).thenReturn(List.of());

        List<SearchResult> results =
                keywordSearchService.search(
                        repositoryId,
                        "   authentication   ",
                        10
                );

        assertTrue(results.isEmpty());

        verify(keywordSearchRepository)
                .search(
                        repositoryId,
                        "authentication",
                        10
                );
    }

    @Test
    void search_shouldRejectNullRepositoryId() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> keywordSearchService.search(
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
                keywordSearchRepository
        );
    }

    @Test
    void search_shouldRejectBlankQuery() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> keywordSearchService.search(
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
                keywordSearchRepository
        );
    }

    @Test
    void search_shouldRejectZeroLimit() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
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

        verifyNoInteractions(
                keywordSearchRepository
        );
    }

    @Test
    void search_shouldRejectLimitAboveMaximum() {

        UUID repositoryId =
                UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
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

        verifyNoInteractions(
                keywordSearchRepository
        );
    }

    @Test
    void search_shouldRejectQueryAboveMaximumLength() {

        UUID repositoryId =
                UUID.randomUUID();

        String query =
                "a".repeat(501);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> keywordSearchService.search(
                                repositoryId,
                                query,
                                10
                        )
                );

        assertEquals(
                "Search query cannot exceed 500 characters",
                exception.getMessage()
        );

        verifyNoInteractions(
                keywordSearchRepository
        );
    }

    @Test
    void search_shouldNotUseLikeWildcardEscaping() {

        UUID repositoryId =
                UUID.randomUUID();

        when(keywordSearchRepository.search(
                repositoryId,
                "User_%",
                10
        )).thenReturn(List.of());

        List<SearchResult> results =
                keywordSearchService.search(
                        repositoryId,
                        "User_%",
                        10
                );

        assertTrue(results.isEmpty());

        verify(keywordSearchRepository)
                .search(
                        repositoryId,
                        "User_%",
                        10
                );
    }
}