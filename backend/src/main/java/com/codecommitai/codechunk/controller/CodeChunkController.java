package com.codecommitai.codechunk.controller;

import com.codecommitai.codechunk.service.CodeChunkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CodeChunkController {

    private final CodeChunkService codeChunkService;

    public CodeChunkController(
            CodeChunkService codeChunkService
    ) {
        this.codeChunkService = codeChunkService;
    }

    @PostMapping("/files/{repositoryFileId}/chunks")
    public ResponseEntity<Map<String, Object>> chunkFile(
            @PathVariable UUID repositoryFileId
    ) {

        int chunkCount =
                codeChunkService.chunkFile(
                        repositoryFileId
                );

        return ResponseEntity.ok(
                Map.of(
                        "repositoryFileId", repositoryFileId,
                        "chunksCreated", chunkCount
                )
        );
    }

    @PostMapping("/repositories/{repositoryId}/chunks")
    public ResponseEntity<Map<String, Object>> chunkRepository(
            @PathVariable UUID repositoryId
    ) {

        int chunkCount =
                codeChunkService.chunkRepository(
                        repositoryId
                );

        return ResponseEntity.ok(
                Map.of(
                        "repositoryId", repositoryId,
                        "chunksCreated", chunkCount
                )
        );
    }
}