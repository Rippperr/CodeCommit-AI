package com.codecommitai.indexing.dto;

import com.codecommitai.indexing.entity.IndexingJob;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IndexingJobResponse(
        UUID id,
        UUID repositoryId,
        String status,
        Integer totalFiles,
        Integer processedFiles,
        Integer totalChunks,
        Integer processedChunks,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt
) {

    public static IndexingJobResponse from(
            IndexingJob job
    ) {
        return new IndexingJobResponse(
                job.getId(),
                job.getRepository().getId(),
                job.getStatus(),
                job.getTotalFiles(),
                job.getProcessedFiles(),
                job.getTotalChunks(),
                job.getProcessedChunks(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}