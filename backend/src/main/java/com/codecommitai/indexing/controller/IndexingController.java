package com.codecommitai.indexing.controller;

import com.codecommitai.indexing.dto.IndexingJobResponse;
import com.codecommitai.indexing.entity.IndexingJob;
import com.codecommitai.indexing.service.IndexingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/indexing")
public class IndexingController {

    private final IndexingService indexingService;

    public IndexingController(
            IndexingService indexingService
    ) {
        this.indexingService = indexingService;
    }

    @PostMapping("/repositories/{repositoryId}")
    public ResponseEntity<IndexingJobResponse> startIndexing(
            @PathVariable UUID repositoryId
    ) {

        IndexingJob job =
                indexingService.startIndexing(
                        repositoryId
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        IndexingJobResponse.from(job)
                );
    }

    @PostMapping("/jobs/{jobId}/execute")
    public ResponseEntity<IndexingJobResponse> executeIndexing(
            @PathVariable UUID jobId
    ) {

        IndexingJob job =
                indexingService.executeIndexing(
                        jobId
                );

        return ResponseEntity.ok(
                IndexingJobResponse.from(job)
        );
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<IndexingJobResponse> getJob(
            @PathVariable UUID jobId
    ) {

        IndexingJob job =
                indexingService.getJob(jobId);

        return ResponseEntity.ok(
                IndexingJobResponse.from(job)
        );
    }

    @GetMapping("/repositories/{repositoryId}/jobs")
    public ResponseEntity<List<IndexingJobResponse>> getRepositoryJobs(
            @PathVariable UUID repositoryId
    ) {

        return ResponseEntity.ok(
                indexingService
                        .getRepositoryJobs(repositoryId)
                        .stream()
                        .map(IndexingJobResponse::from)
                        .toList()
        );
    }
}