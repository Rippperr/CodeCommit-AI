package com.codecommitai.embedding.repository;

import com.codecommitai.embedding.entity.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmbeddingRepository
        extends JpaRepository<Embedding, UUID> {

    Optional<Embedding> findByCodeChunkId(
            UUID codeChunkId
    );

    boolean existsByCodeChunkId(
            UUID codeChunkId
    );
}