
package com.codecommitai.codechunk.repository;

import com.codecommitai.codechunk.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeChunkRepository
        extends JpaRepository<CodeChunk, UUID> {

    List<CodeChunk> findAllByRepositoryFileIdOrderByChunkIndex(
            UUID repositoryFileId
    );

    List<CodeChunk> findAllByRepositoryFileIdAndChunkIndexBetweenOrderByChunkIndex(
            UUID repositoryFileId,
            Integer startChunkIndex,
            Integer endChunkIndex
    );

    List<CodeChunk> findAllByRepositoryFileRepositoryId(
            UUID repositoryId
    );

    void deleteAllByRepositoryFileId(
            UUID repositoryFileId
    );
}
