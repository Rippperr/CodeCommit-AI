package com.codecommitai.search.service;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.service.VectorSearchService;
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
class SemanticSearchServiceTest {

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private EmbeddingProvider embeddingProvider;

    private SemanticSearchService semanticSearchService;

    @BeforeEach
    void setUp() {
        semanticSearchService =
                new SemanticSearchService(
                        vectorSearchService,
                        embeddingProvider
                );
    }

    @Test
    void search_shouldGenerateEmbeddingAndDelegateToVectorSearch() {

        UUID repositoryId = UUID.randomUUID();

        float[] queryVector =
                new float[]{0.1f, 0.2f, 0.3f};

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

        when(embeddingProvider.generateEmbedding(
                "user service"
        )).thenReturn(queryVector);

        when(vectorSearchService.searchByRepository(
                repositoryId,
                queryVector,
                10
        )).thenReturn(List.of(result));

        List<VectorSearchResult> results =
                semanticSearchService.search(
                        repositoryId,
                        "user service",
                        10
                );

        assertEquals(1, results.size());
        assertEquals(result, results.get(0));

        verify(embeddingProvider)
                .generateEmbedding("user service");

        verify(vectorSearchService)
                .searchByRepository(
                        repositoryId,
                        queryVector,
                        10
                );
    }

    @Test
    void search_shouldTrimQueryBeforeGeneratingEmbedding() {

        UUID repositoryId = UUID.randomUUID();

        float[] queryVector =
                new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingProvider.generateEmbedding(
                "user service"
        )).thenReturn(queryVector);

        when(vectorSearchService.searchByRepository(
                repositoryId,
                queryVector,
                10
        )).thenReturn(List.of());

        List<VectorSearchResult> results =
                semanticSearchService.search(
                        repositoryId,
                        "   user service   ",
                        10
                );

        assertTrue(results.isEmpty());

        verify(embeddingProvider)
                .generateEmbedding("user service");

        verify(vectorSearchService)
                .searchByRepository(
                        repositoryId,
                        queryVector,
                        10
                );
    }

    @Test
    void search_shouldUseDefaultLimit() {

        UUID repositoryId = UUID.randomUUID();

        float[] queryVector =
                new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingProvider.generateEmbedding(
                "authentication"
        )).thenReturn(queryVector);

        when(vectorSearchService.searchByRepository(
                repositoryId,
                queryVector,
                10
        )).thenReturn(List.of());

        List<VectorSearchResult> results =
                semanticSearchService.search(
                        repositoryId,
                        "authentication"
                );

        assertTrue(results.isEmpty());

        verify(embeddingProvider)
                .generateEmbedding("authentication");

        verify(vectorSearchService)
                .searchByRepository(
                        repositoryId,
                        queryVector,
                        10
                );
    }

    @Test
    void search_shouldUseCustomLimit() {

        UUID repositoryId = UUID.randomUUID();

        float[] queryVector =
                new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingProvider.generateEmbedding(
                "authentication"
        )).thenReturn(queryVector);

        when(vectorSearchService.searchByRepository(
                repositoryId,
                queryVector,
                25
        )).thenReturn(List.of());

        List<VectorSearchResult> results =
                semanticSearchService.search(
                        repositoryId,
                        "authentication",
                        25
                );

        assertTrue(results.isEmpty());

        verify(vectorSearchService)
                .searchByRepository(
                        repositoryId,
                        queryVector,
                        25
                );
    }

    @Test
    void search_shouldRejectNullRepositoryId() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> semanticSearchService.search(
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
                embeddingProvider,
                vectorSearchService
        );
    }

    @Test
    void search_shouldRejectBlankQuery() {

        UUID repositoryId = UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> semanticSearchService.search(
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
                embeddingProvider,
                vectorSearchService
        );
    }

    @Test
    void search_shouldRejectZeroLimit() {

        UUID repositoryId = UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> semanticSearchService.search(
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
                embeddingProvider,
                vectorSearchService
        );
    }

    @Test
    void search_shouldRejectLimitAboveMaximum() {

        UUID repositoryId = UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> semanticSearchService.search(
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
                embeddingProvider,
                vectorSearchService
        );
    }

    @Test
    void search_shouldRejectQueryAboveMaximumLength() {

        UUID repositoryId = UUID.randomUUID();

        String query =
                "a".repeat(501);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> semanticSearchService.search(
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
                embeddingProvider,
                vectorSearchService
        );
    }
}
