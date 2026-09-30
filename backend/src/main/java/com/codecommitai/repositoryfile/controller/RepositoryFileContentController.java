package com.codecommitai.repositoryfile.controller;

import com.codecommitai.repositoryfile.service.RepositoryFileContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryFileContentController {

    private final RepositoryFileContentService repositoryFileContentService;

    public RepositoryFileContentController(
            RepositoryFileContentService repositoryFileContentService
    ) {
        this.repositoryFileContentService =
                repositoryFileContentService;
    }

    @PostMapping("/{repositoryId}/files/download")
    public ResponseEntity<Map<String, Object>> downloadContents(
            @PathVariable UUID repositoryId
    ) {

        int downloadedCount =
                repositoryFileContentService.downloadContents(
                        repositoryId
                );

        return ResponseEntity.ok(
                Map.of(
                        "repositoryId", repositoryId,
                        "downloadedFiles", downloadedCount
                )
        );
    }
}