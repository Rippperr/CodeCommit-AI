package com.codecommitai.embedding.service;

import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.repository.EmbeddingRepository;
import com.codecommitai.embedding.repository.VectorSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VectorSearchServiceRepositoryTest {

    private static final UUID REPOSITORY_ID =
            UUID.fromString(
                    "a2d08518-be93-4ba0-8931-76d50eef3fd3"
            );

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Autowired
    private VectorSearchRepository vectorSearchRepository;

    @Autowired
    private EmbeddingRepository embeddingRepository;

    @Test
    void searchByRepository_shouldReturnResultsFromSelectedRepository() {

        String query = "repository service";

        float[] queryVector =
                embeddingProvider.generateEmbedding(query);

        List<VectorSearchResult> results =
                vectorSearchService.searchByRepository(
                        REPOSITORY_ID,
                        queryVector,
                        10
                );

        assertFalse(
                results.isEmpty(),
                "Semantic search should return results"
        );

        assertTrue(
                results.size() <= 10,
                "Result count must not exceed the requested limit"
        );

        for (VectorSearchResult result : results) {

            assertNotNull(result.filePath());
            assertNotNull(result.fileName());
            assertNotNull(result.chunkIndex());
            assertNotNull(result.startLine());
            assertNotNull(result.endLine());
            assertNotNull(result.content());
            assertNotNull(result.cosineDistance());
        }
    }

    @Test
    void searchByRepository_shouldRespectLimit() {

        String query = "repository";

        float[] queryVector =
                embeddingProvider.generateEmbedding(query);

        List<VectorSearchResult> results =
                vectorSearchService.searchByRepository(
                        REPOSITORY_ID,
                        queryVector,
                        5
                );

        assertTrue(
                results.size() <= 5,
                "Semantic search must respect the requested limit"
        );
    }

    @Test
    void searchByRepository_shouldRejectNullRepositoryId() {

        float[] queryVector =
                embeddingProvider.generateEmbedding("repository");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> vectorSearchService.searchByRepository(
                                null,
                                queryVector,
                                10
                        )
                );

        assertEquals(
                "Repository ID cannot be null",
                exception.getMessage()
        );
    }
}
