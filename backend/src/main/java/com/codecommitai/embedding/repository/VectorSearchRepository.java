package com.codecommitai.embedding.repository;

import com.codecommitai.embedding.entity.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VectorSearchRepository
        extends JpaRepository<Embedding, UUID> {

    @Query(value = """
            SELECT e.id
            FROM embeddings e
            ORDER BY e.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findNearestEmbeddingIds(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT e.id
            FROM embeddings e
            INNER JOIN code_chunks c
                ON c.id = e.code_chunk_id
            INNER JOIN repository_files rf
                ON rf.id = c.repository_file_id
            WHERE rf.repository_id = :repositoryId
            ORDER BY e.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findNearestEmbeddingIdsByRepository(
            @Param("repositoryId") UUID repositoryId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT e.embedding <=> CAST(:queryVector AS vector)
            FROM embeddings e
            WHERE e.id = :embeddingId
            """, nativeQuery = true)
    Double calculateDistance(
            @Param("embeddingId") UUID embeddingId,
            @Param("queryVector") String queryVector
    );
}