package com.codecommitai.indexing.service;

import com.codecommitai.indexing.entity.IndexingJob;
import com.codecommitai.indexing.repository.IndexingJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class IndexingJobStateService {

    private final IndexingJobRepository indexingJobRepository;

    public IndexingJobStateService(
            IndexingJobRepository indexingJobRepository
    ) {
        this.indexingJobRepository = indexingJobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IndexingJob markRunning(UUID jobId) {

        IndexingJob job = findJob(jobId);

        job.setStatus(IndexingService.STATUS_RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        job.setCompletedAt(null);
        job.setErrorMessage(null);

        return indexingJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IndexingJob updateFileProgress(
            UUID jobId,
            int totalFiles
    ) {

        IndexingJob job = findJob(jobId);

        job.setTotalFiles(totalFiles);
        job.setProcessedFiles(0);

        return indexingJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IndexingJob updateChunkProgress(
            UUID jobId,
            int totalChunks
    ) {

        IndexingJob job = findJob(jobId);

        job.setTotalChunks(totalChunks);
        job.setProcessedChunks(0);

        return indexingJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IndexingJob markCompleted(
            UUID jobId,
            int totalFiles,
            int totalChunks,
            int processedChunks
    ) {

        IndexingJob job = findJob(jobId);

        job.setTotalFiles(totalFiles);
        job.setProcessedFiles(totalFiles);
        job.setTotalChunks(totalChunks);
        job.setProcessedChunks(processedChunks);

        job.setStatus(IndexingService.STATUS_COMPLETED);
        job.setCompletedAt(OffsetDateTime.now());
        job.setErrorMessage(null);

        return indexingJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IndexingJob markFailed(
            UUID jobId,
            Exception exception
    ) {

        IndexingJob job = findJob(jobId);

        job.setStatus(IndexingService.STATUS_FAILED);
        job.setErrorMessage(buildErrorMessage(exception));
        job.setCompletedAt(OffsetDateTime.now());

        return indexingJobRepository.save(job);
    }

    private IndexingJob findJob(UUID jobId) {

        return indexingJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Indexing job not found: " + jobId
                        )
                );
    }

    private String buildErrorMessage(
            Exception exception
    ) {

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return exception.getClass().getSimpleName()
                + ": "
                + message;
    }
}
