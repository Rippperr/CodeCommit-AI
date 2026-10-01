package com.codecommitai.indexing.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.codechunk.service.CodeChunkService;
import com.codecommitai.embedding.service.EmbeddingService;
import com.codecommitai.indexing.entity.IndexingJob;
import com.codecommitai.indexing.repository.IndexingJobRepository;
import com.codecommitai.repository.dto.RepositoryFileChangeSet;
import com.codecommitai.repository.entity.Repository;
import com.codecommitai.repository.repository.RepositoryRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.repository.RepositoryFileRepository;
import com.codecommitai.repositoryfile.service.RepositoryFileContentService;
import com.codecommitai.repositoryfile.service.RepositoryFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IndexingService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private final RepositoryRepository repositoryRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final IndexingJobRepository indexingJobRepository;
    private final RepositoryFileService repositoryFileService;
    private final RepositoryFileContentService repositoryFileContentService;
    private final CodeChunkService codeChunkService;
    private final EmbeddingService embeddingService;
    private final IndexingJobStateService jobStateService;

    public IndexingService(
            RepositoryRepository repositoryRepository,
            RepositoryFileRepository repositoryFileRepository,
            IndexingJobRepository indexingJobRepository,
            RepositoryFileService repositoryFileService,
            RepositoryFileContentService repositoryFileContentService,
            CodeChunkService codeChunkService,
            EmbeddingService embeddingService,
            IndexingJobStateService jobStateService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.indexingJobRepository = indexingJobRepository;
        this.repositoryFileService = repositoryFileService;
        this.repositoryFileContentService = repositoryFileContentService;
        this.codeChunkService = codeChunkService;
        this.embeddingService = embeddingService;
        this.jobStateService = jobStateService;
    }

    @Transactional
    public IndexingJob startIndexing(UUID repositoryId) {

        Repository repository =
                repositoryRepository.findById(repositoryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Repository not found: " + repositoryId
                                )
                        );

        IndexingJob job = new IndexingJob();

        job.setRepository(repository);
        job.setStatus(STATUS_PENDING);
        job.setTotalFiles(0);
        job.setProcessedFiles(0);
        job.setTotalChunks(0);
        job.setProcessedChunks(0);

        return indexingJobRepository.save(job);
    }

    public IndexingJob executeIndexing(UUID jobId) {

        IndexingJob job = getJob(jobId);

        jobStateService.markRunning(jobId);

        try {

            UUID repositoryId =
                    job.getRepository().getId();

            /*
             * Discover the current GitHub state.
             *
             * githubSha remains the SHA of the last successfully
             * indexed version.
             *
             * New GitHub SHAs are stored only in pendingGithubSha.
             */
            RepositoryFileChangeSet changeSet =
                    repositoryFileService
                            .discoverFilesWithChanges(repositoryId);

            List<RepositoryFile> filesToProcess =
                    new ArrayList<>();

            filesToProcess.addAll(changeSet.newFiles());
            filesToProcess.addAll(changeSet.modifiedFiles());

            int totalFiles =
                    changeSet.totalFiles();

            jobStateService.updateFileProgress(
                    jobId,
                    totalFiles
            );

            /*
             * Delete repository files that disappeared from GitHub.
             *
             * Database cascade relationships handle the associated
             * chunks/embeddings according to the configured schema.
             */
            for (RepositoryFile deletedFile :
                    changeSet.deletedFiles()) {

                repositoryFileRepository.delete(deletedFile);
            }

            repositoryFileRepository.flush();

            /*
             * Download content using pendingGithubSha.
             */
            if (!filesToProcess.isEmpty()) {

                repositoryFileContentService.downloadContents(
                        repositoryId,
                        filesToProcess
                );
            }

            /*
             * Recreate chunks for every new/modified file.
             */
            int totalChunks = 0;

            for (RepositoryFile file :
                    filesToProcess) {

                totalChunks +=
                        codeChunkService.chunkFile(
                                file.getId()
                        );
            }

            jobStateService.updateChunkProgress(
                    jobId,
                    totalChunks
            );

            /*
             * Generate embeddings for the newly created chunks.
             */
            int embeddingsGenerated = 0;

            for (RepositoryFile file :
                    filesToProcess) {

                List<CodeChunk> chunks =
                        codeChunkService.getChunksForFile(
                                file.getId()
                        );

                for (CodeChunk chunk : chunks) {

                    embeddingService.generateForChunk(
                            chunk.getId()
                    );

                    embeddingsGenerated++;
                }
            }

            /*
             * CRITICAL:
             *
             * Only after content download, chunking and embedding
             * generation have all succeeded do we commit the newly
             * discovered GitHub SHAs.
             *
             * If anything above fails, githubSha remains unchanged
             * and the next indexing run will retry the file.
             */
            repositoryFileService.markFilesAsIndexed(
                    filesToProcess
            );

            return jobStateService.markCompleted(
                    jobId,
                    totalFiles,
                    totalChunks,
                    embeddingsGenerated
            );

        } catch (Exception exception) {

            jobStateService.markFailed(
                    jobId,
                    exception
            );

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public IndexingJob getJob(UUID jobId) {

        return indexingJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Indexing job not found: " + jobId
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<IndexingJob> getRepositoryJobs(
            UUID repositoryId
    ) {

        return indexingJobRepository
                .findAllByRepositoryIdOrderByCreatedAtDesc(
                        repositoryId
                );
    }
}