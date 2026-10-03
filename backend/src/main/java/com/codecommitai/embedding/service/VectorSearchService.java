package com.codecommitai.embedding.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.embedding.dto.VectorSearchResult;
import com.codecommitai.embedding.entity.Embedding;
import com.codecommitai.embedding.provider.EmbeddingProvider;
import com.codecommitai.embedding.repository.EmbeddingRepository;
import com.codecommitai.embedding.repository.VectorSearchRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VectorSearchService {

    private static final int MAX_SEARCH_LIMIT = 50;

    private final VectorSearchRepository vectorSearchRepository;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingProvider embeddingProvider;

    public VectorSearchService(
            VectorSearchRepository vectorSearchRepository,
            EmbeddingRepository embeddingRepository,
            EmbeddingProvider embeddingProvider
    ) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingProvider = embeddingProvider;
    }

    @Transactional(readOnly = true)
    public List<VectorSearchResult> search(
            float[] queryVector,
            int limit
    ) {

        validateQuery(queryVector, limit);

        String queryVectorString =
                toVectorString(queryVector);

        List<UUID> embeddingIds =
                vectorSearchRepository.findNearestEmbeddingIds(
                        queryVectorString,
                        limit
                );

        List<VectorSearchResult> results =
                new ArrayList<>();

        for (UUID embeddingId : embeddingIds) {

            embeddingRepository
                    .findById(embeddingId)
                    .ifPresent(embedding -> {

                        Double distance =
                                vectorSearchRepository.calculateDistance(
                                        embeddingId,
                                        queryVectorString
                                );

                        results.add(
                                toResult(
                                        embedding,
                                        distance
                                )
                        );
                    });
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<VectorSearchResult> searchByRepository(
            UUID repositoryId,
            float[] queryVector,
            int limit
    ) {

        validateRepositoryId(repositoryId);
        validateQuery(queryVector, limit);

        String queryVectorString =
                toVectorString(queryVector);

        List<UUID> embeddingIds =
                vectorSearchRepository.findNearestEmbeddingIdsByRepository(
                        repositoryId,
                        queryVectorString,
                        limit
                );

        List<VectorSearchResult> results =
                new ArrayList<>();

        for (UUID embeddingId : embeddingIds) {

            embeddingRepository
                    .findById(embeddingId)
                    .ifPresent(embedding -> {

                        Double distance =
                                vectorSearchRepository.calculateDistance(
                                        embeddingId,
                                        queryVectorString
                                );

                        results.add(
                                toResult(
                                        embedding,
                                        distance
                                )
                        );
                    });
        }

        return results;
    }

    private VectorSearchResult toResult(
            Embedding embedding,
            Double distance
    ) {

        CodeChunk chunk =
                embedding.getCodeChunk();

        RepositoryFile file =
                chunk.getRepositoryFile();

        return new VectorSearchResult(
                file.getPath(),
                file.getFileName(),
                chunk.getChunkIndex(),
                chunk.getStartLine(),
                chunk.getEndLine(),
                chunk.getContent(),
                distance
        );
    }

    private void validateRepositoryId(
            UUID repositoryId
    ) {

        if (repositoryId == null) {

            throw new IllegalArgumentException(
                    "Repository ID cannot be null"
            );
        }
    }

    private void validateQuery(
            float[] queryVector,
            int limit
    ) {

        if (queryVector == null ||
                queryVector.length == 0) {

            throw new IllegalArgumentException(
                    "Query vector cannot be null or empty"
            );
        }

        int expectedDimensions =
                embeddingProvider.getDimensions();

        if (queryVector.length != expectedDimensions) {

            throw new IllegalArgumentException(
                    "Query vector must contain exactly "
                            + expectedDimensions
                            + " dimensions"
            );
        }

        if (limit <= 0) {

            throw new IllegalArgumentException(
                    "Limit must be greater than zero"
            );
        }

        if (limit > MAX_SEARCH_LIMIT) {

            throw new IllegalArgumentException(
                    "Limit cannot be greater than "
                            + MAX_SEARCH_LIMIT
            );
        }
    }

    private String toVectorString(
            float[] vector
    ) {

        StringBuilder builder =
                new StringBuilder("[");

        for (int i = 0; i < vector.length; i++) {

            if (i > 0) {
                builder.append(",");
            }

            builder.append(vector[i]);
        }

        builder.append("]");

        return builder.toString();
    }
}