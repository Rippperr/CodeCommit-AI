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

    @Transactional
    public int downloadContents(UUID repositoryId) {

        Repository repository =
                repositoryRepository.findById(repositoryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Repository not found: " + repositoryId
                                )
                        );

        List<RepositoryFile> files =
                repositoryFileRepository.findAllByRepositoryId(repositoryId);

        return downloadContents(repository, files);
    }

    @Transactional
    public int downloadContents(
            UUID repositoryId,
            List<RepositoryFile> files
    ) {

        Repository repository =
                repositoryRepository.findById(repositoryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Repository not found: " + repositoryId
                                )
                        );

        return downloadContents(repository, files);
    }

    private int downloadContents(
            Repository repository,
            List<RepositoryFile> files
    ) {

        int downloadedCount = 0;

        if (files == null || files.isEmpty()) {
            return downloadedCount;
        }

        for (RepositoryFile file : files) {

            /*
             * IMPORTANT:
             *
             * We use pendingGithubSha here instead of githubSha.
             *
             * githubSha represents the last successfully indexed version.
             * pendingGithubSha represents the newly discovered GitHub version.
             */
            String sha = file.getPendingGithubSha();

            if (sha == null || sha.isBlank()) {
                continue;
            }

            GitHubBlobResponse blob =
                    gitHubService.getBlob(
                            repository.getOwner(),
                            repository.getName(),
                            sha
                    );

            if (blob == null) {
                continue;
            }

            String decodedContent = decodeContent(blob);

            file.setContent(decodedContent);

            repositoryFileRepository.save(file);

            downloadedCount++;
        }

        return downloadedCount;
    }

    private String decodeContent(GitHubBlobResponse blob) {

        if (blob.content() == null || blob.content().isBlank()) {
            return "";
        }

        String encoding = blob.encoding();

        if (encoding == null || encoding.isBlank()) {
            throw new IllegalStateException(
                    "GitHub blob encoding is missing"
            );
        }

        if (!"base64".equalsIgnoreCase(encoding)) {
            throw new IllegalStateException(
                    "Unsupported GitHub blob encoding: " + encoding
            );
        }

        String normalizedContent =
                blob.content()
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace(" ", "");

        try {

            byte[] decoded =
                    Base64.getDecoder().decode(normalizedContent);

            return new String(
                    decoded,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Failed to decode GitHub blob content",
                    exception
            );
        }
    }
}