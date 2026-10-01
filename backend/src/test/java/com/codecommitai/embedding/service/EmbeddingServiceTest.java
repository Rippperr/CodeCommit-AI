package com.codecommitai.embedding.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.codechunk.repository.CodeChunkRepository;
import com.codecommitai.embedding.entity.Embedding;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.repository.EmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private CodeChunkRepository codeChunkRepository;

    @Mock
    private EmbeddingRepository embeddingRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService =
                new EmbeddingService(
                        codeChunkRepository,
                        embeddingRepository,
                        embeddingProvider
                );
    }

    @Test
    void generateForChunk_shouldGenerateAndPersistEmbedding() {
        UUID chunkId = UUID.randomUUID();

        CodeChunk chunk = new CodeChunk();
        chunk.setContent("public class Example {}");

        float[] vector = new float[1536];
        vector[0] = 0.25f;

        when(codeChunkRepository.findById(chunkId))
                .thenReturn(Optional.of(chunk));

        when(embeddingProvider.generateEmbedding(
                chunk.getContent()
        )).thenReturn(vector);

        when(embeddingProvider.getModelName())
                .thenReturn("gemini-embedding-2");

        when(embeddingProvider.getDimensions())
                .thenReturn(1536);

        when(embeddingRepository.findByCodeChunkId(chunkId))
                .thenReturn(Optional.empty());

        Embedding savedEmbedding = new Embedding();

        when(embeddingRepository.save(any(Embedding.class)))
                .thenReturn(savedEmbedding);

        Embedding result =
                embeddingService.generateForChunk(chunkId);

        assertSame(savedEmbedding, result);

        ArgumentCaptor<Embedding> captor =
                ArgumentCaptor.forClass(Embedding.class);

        verify(embeddingRepository)
                .save(captor.capture());

        Embedding persisted =
                captor.getValue();

        assertSame(chunk, persisted.getCodeChunk());
        assertSame(vector, persisted.getEmbedding());
        assertEquals(
                "gemini-embedding-2",
                persisted.getModel()
        );
        assertEquals(
                1536,
                persisted.getDimensions()
        );
    }

    @Test
    void generateForChunk_shouldRejectDimensionMismatch() {
        UUID chunkId = UUID.randomUUID();

        CodeChunk chunk = new CodeChunk();
        chunk.setContent("public class Example {}");

        float[] invalidVector = new float[768];

        when(codeChunkRepository.findById(chunkId))
                .thenReturn(Optional.of(chunk));

        when(embeddingProvider.generateEmbedding(
                chunk.getContent()
        )).thenReturn(invalidVector);

        when(embeddingProvider.getDimensions())
                .thenReturn(1536);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> embeddingService.generateForChunk(chunkId)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Expected 1536")
        );

        verify(embeddingRepository, never())
                .save(any(Embedding.class));
    }

    @Test
    void generateForChunk_shouldRejectMissingChunk() {
        UUID chunkId = UUID.randomUUID();

        when(codeChunkRepository.findById(chunkId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> embeddingService.generateForChunk(chunkId)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Code chunk not found")
        );

        verifyNoInteractions(embeddingProvider);
        verifyNoInteractions(embeddingRepository);
    }

    @Test
    void generateForRepository_shouldProcessAllChunksInBatch() {
        UUID repositoryId = UUID.randomUUID();

        CodeChunk firstChunk = new CodeChunk();
        firstChunk.setContent("public class First {}");

        CodeChunk secondChunk = new CodeChunk();
        secondChunk.setContent("public class Second {}");

        List<CodeChunk> chunks =
                List.of(firstChunk, secondChunk);

        float[] firstVector = new float[1536];
        float[] secondVector = new float[1536];

        when(codeChunkRepository
                .findAllByRepositoryFileRepositoryId(repositoryId))
                .thenReturn(chunks);

        when(embeddingProvider.getModelName())
                .thenReturn("gemini-embedding-2");

        when(embeddingProvider.getDimensions())
                .thenReturn(1536);

        when(embeddingProvider.generateEmbeddings(
                List.of(
                        firstChunk.getContent(),
                        secondChunk.getContent()
                )
        )).thenReturn(
                List.of(firstVector, secondVector)
        );

        when(embeddingRepository.findByCodeChunkId(any()))
                .thenReturn(Optional.empty());

        when(embeddingRepository.save(any(Embedding.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        int generatedCount =
                embeddingService.generateForRepository(
                        repositoryId
                );

        assertEquals(2, generatedCount);

        verify(embeddingProvider)
                .generateEmbeddings(
                        List.of(
                                firstChunk.getContent(),
                                secondChunk.getContent()
                        )
                );

        verify(embeddingRepository, times(2))
                .save(any(Embedding.class));
    }

    @Test
    void generateForRepository_shouldReturnZeroWhenNoChunksExist() {
        UUID repositoryId = UUID.randomUUID();

        when(codeChunkRepository
                .findAllByRepositoryFileRepositoryId(repositoryId))
                .thenReturn(List.of());

        int generatedCount =
                embeddingService.generateForRepository(
                        repositoryId
                );

        assertEquals(0, generatedCount);

        verifyNoInteractions(embeddingProvider);
        verifyNoInteractions(embeddingRepository);
    }
}