package com.codecommitai.github.controller;

import com.codecommitai.github.dto.GitHubContentResponse;
import com.codecommitai.github.dto.GitHubRepositoryResponse;
import com.codecommitai.github.dto.GitHubTreeResponse;
import com.codecommitai.github.dto.ImportRepositoryRequest;
import com.codecommitai.github.service.GitHubService;
import com.codecommitai.repository.dto.RepositoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/repositories/{owner}/{repositoryName}")
    public GitHubRepositoryResponse getRepository(
            @PathVariable String owner,
            @PathVariable String repositoryName
    ) {
        return gitHubService.getRepository(
                owner,
                repositoryName
        );
    }

    @PostMapping("/repositories/import")
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse importRepository(
            @Valid @RequestBody ImportRepositoryRequest request
    ) {
        return gitHubService.importRepository(request);
    }

    @GetMapping({
            "/repositories/{owner}/{repositoryName}/contents",
            "/repositories/{owner}/{repositoryName}/contents/{*path}"
    })
    public GitHubContentResponse[] getContents(
            @PathVariable String owner,
            @PathVariable String repositoryName,
            @PathVariable(required = false) String path
    ) {
        return gitHubService.getContents(
                owner,
                repositoryName,
                path == null ? "" : path
        );
    }

    @GetMapping("/repositories/{owner}/{repositoryName}/tree")
    public GitHubTreeResponse getRepositoryTree(
            @PathVariable String owner,
            @PathVariable String repositoryName,
            @RequestParam(defaultValue = "main") String branch
    ) {
        return gitHubService.getRepositoryTree(
                owner,
                repositoryName,
                branch
        );
    }
}