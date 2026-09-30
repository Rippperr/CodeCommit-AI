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
                                        "Repository not found: "
                                                + repositoryId
                                )
                        );

        IndexingJob job = new IndexingJob();

        job.setRepository(repository);
        job.setStatus(STATUS_PENDING);

        return indexingJobRepository.save(job);
    }

    public IndexingJob executeIndexing(UUID jobId) {

        IndexingJob job = getJob(jobId);

        jobStateService.markRunning(jobId);

        try {

            UUID repositoryId =
                    job.getRepository().getId();

            /*
             * Discover the current GitHub tree and classify files into:
             *
             * NEW
             * MODIFIED
             * UNCHANGED
             * DELETED
             */
            RepositoryFileChangeSet changeSet =
                    repositoryFileService.discoverFilesWithChanges(
                            repositoryId
                    );

            List<RepositoryFile> filesToProcess =
                    new ArrayList<>();

            filesToProcess.addAll(changeSet.newFiles());
            filesToProcess.addAll(changeSet.modifiedFiles());

            /*
             * Total file count represents the current repository state,
             * including files that were unchanged.
             */
            int totalFiles =
                    changeSet.totalFiles();

            jobStateService.updateFileProgress(
                    jobId,
                    totalFiles
            );

            /*
             * Remove files that no longer exist in GitHub.
             *
             * repository_files -> code_chunks -> embeddings
             * are connected using ON DELETE CASCADE.
             */
            for (RepositoryFile deletedFile :
                    changeSet.deletedFiles()) {

                repositoryFileRepository.delete(deletedFile);
            }

            repositoryFileRepository.flush();

            /*
             * Download content only for new and modified files.
             */
            if (!filesToProcess.isEmpty()) {

                repositoryFileContentService.downloadContents(
                        repositoryId,
                        filesToProcess
                );
            }

            /*
             * Chunk only new and modified files.
             *
             * Unchanged files keep their existing chunks.
             */
            int totalChunks = 0;

            for (RepositoryFile file : filesToProcess) {

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
             * Generate embeddings only for chunks belonging to
             * new/modified files.
             */
            int embeddingsGenerated = 0;

            for (RepositoryFile file : filesToProcess) {

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
             * All current files are considered processed from the
             * indexing job's perspective. Deleted files are no longer
             * part of the repository.
             */
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
                                "Indexing job not found: "
                                        + jobId
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<IndexingJob> getRepositoryJobs(
            UUID repositoryId
    ) {

        if (!repositoryRepository.existsById(repositoryId)) {
            throw new IllegalArgumentException(
                    "Repository not found: "
                            + repositoryId
            );
        }

        return indexingJobRepository
                .findAllByRepositoryIdOrderByCreatedAtDesc(
                        repositoryId
                );
    }
}