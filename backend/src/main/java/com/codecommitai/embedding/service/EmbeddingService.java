package com.codecommitai.embedding.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.codechunk.repository.CodeChunkRepository;
import com.codecommitai.embedding.entity.Embedding;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.repository.EmbeddingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingService {

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingProvider embeddingProvider;

    public EmbeddingService(
            CodeChunkRepository codeChunkRepository,
            EmbeddingRepository embeddingRepository,
            EmbeddingProvider embeddingProvider
    ) {
        this.codeChunkRepository = codeChunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingProvider = embeddingProvider;
    }

    @Transactional
    public Embedding generateForChunk(UUID codeChunkId) {

        CodeChunk codeChunk =
                codeChunkRepository.findById(codeChunkId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Code chunk not found: " + codeChunkId
                        ));

        float[] vector =
                embeddingProvider.generateEmbedding(
                        codeChunk.getContent()
                );

        validateDimensions(vector);

        Embedding embedding =
                embeddingRepository
                        .findByCodeChunkId(codeChunkId)
                        .orElseGet(Embedding::new);

        embedding.setCodeChunk(codeChunk);
        embedding.setEmbedding(vector);
        embedding.setModel(
                embeddingProvider.getModelName()
        );

        return embeddingRepository.save(embedding);
    }

    @Transactional
    public int generateForRepository(UUID repositoryId) {

        List<CodeChunk> chunks =
                codeChunkRepository
                        .findAllByRepositoryFileRepositoryId(
                                repositoryId
                        );

        int generatedCount = 0;

        for (CodeChunk chunk : chunks) {
            generateForChunk(chunk.getId());
            generatedCount++;
        }

        return generatedCount;
    }

    private void validateDimensions(float[] vector) {

        if (vector == null) {
            throw new IllegalStateException(
                    "Embedding provider returned null"
            );
        }

        int expectedDimensions =
                embeddingProvider.getDimensions();

        if (vector.length != expectedDimensions) {
            throw new IllegalStateException(
                    "Invalid embedding dimensions. Expected "
                            + expectedDimensions
                            + " but received "
                            + vector.length
            );
        }
    }
}