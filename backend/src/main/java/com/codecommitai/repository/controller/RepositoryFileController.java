package com.codecommitai.repositoryfile.controller;

import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.service.RepositoryFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryFileController {

    private final RepositoryFileService repositoryFileService;

    public RepositoryFileController(
            RepositoryFileService repositoryFileService
    ) {
        this.repositoryFileService = repositoryFileService;
    }

    @PostMapping("/{repositoryId}/files/discover")
    public ResponseEntity<List<RepositoryFile>> discoverFiles(
            @PathVariable UUID repositoryId
    ) {
        return ResponseEntity.ok(
                repositoryFileService.discoverFiles(repositoryId)
        );
    }
}