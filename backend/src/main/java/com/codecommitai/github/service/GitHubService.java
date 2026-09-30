package com.codecommitai.github.service;

import com.codecommitai.github.client.GitHubClient;
import com.codecommitai.github.dto.GitHubBlobResponse;
import com.codecommitai.github.dto.GitHubContentResponse;
import com.codecommitai.github.dto.GitHubRepositoryResponse;
import com.codecommitai.github.dto.GitHubTreeResponse;
import com.codecommitai.github.dto.ImportRepositoryRequest;
import com.codecommitai.repository.dto.CreateRepositoryRequest;
import com.codecommitai.repository.dto.RepositoryResponse;
import com.codecommitai.repository.service.RepositoryService;
import org.springframework.stereotype.Service;

@Service
public class GitHubService {

    private final GitHubClient gitHubClient;
    private final RepositoryService repositoryService;

    public GitHubService(
            GitHubClient gitHubClient,
            RepositoryService repositoryService
    ) {
        this.gitHubClient = gitHubClient;
        this.repositoryService = repositoryService;
    }

    public GitHubRepositoryResponse getRepository(
            String owner,
            String repositoryName
    ) {
        return gitHubClient.getRepository(
                owner,
                repositoryName
        );
    }

    public RepositoryResponse importRepository(
            ImportRepositoryRequest request
    ) {
        GitHubRepositoryResponse githubRepository =
                gitHubClient.getRepository(
                        request.owner(),
                        request.repositoryName()
                );

        CreateRepositoryRequest repositoryRequest =
                new CreateRepositoryRequest(
                        githubRepository.id(),
                        githubRepository.owner().login(),
                        githubRepository.name(),
                        githubRepository.fullName(),
                        githubRepository.defaultBranch(),
                        githubRepository.description(),
                        githubRepository.htmlUrl(),
                        githubRepository.privateRepository()
                );

        return repositoryService.create(repositoryRequest);
    }

    public GitHubContentResponse[] getContents(
            String owner,
            String repositoryName,
            String path
    ) {
        return gitHubClient.getContents(
                owner,
                repositoryName,
                path
        );
    }

    public GitHubTreeResponse getRepositoryTree(
            String owner,
            String repositoryName,
            String branch
    ) {
        return gitHubClient.getRepositoryTree(
                owner,
                repositoryName,
                branch
        );
    }

    public GitHubBlobResponse getBlob(
            String owner,
            String repositoryName,
            String fileSha
    ) {
        return gitHubClient.getBlob(
                owner,
                repositoryName,
                fileSha
        );
    }
}