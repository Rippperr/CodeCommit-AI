package com.codecommitai.embedding.controller;

import com.codecommitai.embedding.entity.Embedding;
import com.codecommitai.embedding.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(
            EmbeddingService embeddingService
    ) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/chunks/{chunkId}/embedding")
    public ResponseEntity<Map<String, Object>> generateForChunk(
            @PathVariable UUID chunkId
    ) {

        Embedding embedding =
                embeddingService.generateForChunk(chunkId);

        return ResponseEntity.ok(
                Map.of(
                        "embeddingId", embedding.getId(),
                        "codeChunkId", chunkId,
                        "model", embedding.getModel(),
                        "dimensions",
                        embedding.getEmbedding().length
                )
        );
    }

    @PostMapping("/repositories/{repositoryId}/embeddings")
    public ResponseEntity<Map<String, Object>> generateForRepository(
            @PathVariable UUID repositoryId
    ) {

        int generatedCount =
                embeddingService.generateForRepository(
                        repositoryId
                );

        return ResponseEntity.ok(
                Map.of(
                        "repositoryId", repositoryId,
                        "embeddingsGenerated", generatedCount
                )
        );
    }
}