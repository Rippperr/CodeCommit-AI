package com.codecommitai.repositoryfile.service;

import com.codecommitai.github.dto.GitHubBlobResponse;
import com.codecommitai.github.service.GitHubService;
import com.codecommitai.repository.entity.Repository;
import com.codecommitai.repository.repository.RepositoryRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.repository.RepositoryFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RepositoryFileContentService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final GitHubService gitHubService;

    public RepositoryFileContentService(
            RepositoryRepository repositoryRepository,
            RepositoryFileRepository repositoryFileRepository,
            GitHubService gitHubService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.gitHubService = gitHubService;
    }

    /**
     * Downloads content for all files in a repository.
     *
     * Kept for compatibility with the existing API.
     */
    @Transactional
    public int downloadContents(UUID repositoryId) {

        Repository repository = repositoryRepository
                .findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Repository not found: " + repositoryId
                ));

        List<RepositoryFile> files =
                repositoryFileRepository.findAllByRepositoryId(
                        repositoryId
                );

        return downloadContents(
                repository,
                files
        );
    }

    /**
     * Downloads content only for the supplied files.
     *
     * This is used by incremental indexing so unchanged files
     * are not downloaded again.
     */
    @Transactional
    public int downloadContents(
            UUID repositoryId,
            List<RepositoryFile> files
    ) {

        Repository repository = repositoryRepository
                .findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Repository not found: " + repositoryId
                ));

        return downloadContents(
                repository,
                files
        );
    }

    private int downloadContents(
            Repository repository,
            List<RepositoryFile> files
    ) {

        int downloadedCount = 0;

        for (RepositoryFile file : files) {

            if (file.getGithubSha() == null ||
                    file.getGithubSha().isBlank()) {
                continue;
            }

            GitHubBlobResponse blob =
                    gitHubService.getBlob(
                            repository.getOwner(),
                            repository.getName(),
                            file.getGithubSha()
                    );

            if (blob == null) {
                continue;
            }

            String decodedContent =
                    decodeContent(blob);

            file.setContent(decodedContent);

            repositoryFileRepository.save(file);

            downloadedCount++;
        }

        return downloadedCount;
    }

    private String decodeContent(GitHubBlobResponse blob) {

        if (blob.content() == null ||
                blob.content().isBlank()) {
            return "";
        }

        if (!"base64".equalsIgnoreCase(blob.encoding())) {
            throw new IllegalStateException(
                    "Unsupported GitHub blob encoding: "
                            + blob.encoding()
            );
        }

        String normalizedContent =
                blob.content()
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace(" ", "");

        byte[] decodedBytes =
                Base64.getDecoder().decode(
                        normalizedContent
                );

        return new String(
                decodedBytes,
                StandardCharsets.UTF_8
        );
    }
}