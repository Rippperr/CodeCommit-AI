package com.codecommitai.github.client;

import com.codecommitai.github.dto.GitHubBlobResponse;
import com.codecommitai.github.dto.GitHubContentResponse;
import com.codecommitai.github.dto.GitHubRepositoryResponse;
import com.codecommitai.github.dto.GitHubTreeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GitHubClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token}") String token
    ) {
        this.objectMapper = objectMapper;

        RestClient.Builder builder = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(
                        "Accept",
                        "application/vnd.github+json"
                )
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2026-03-10"
                );

        if (token != null && !token.isBlank()) {
            builder.defaultHeader(
                    "Authorization",
                    "Bearer " + token
            );
        }

        this.restClient = builder.build();
    }

    public GitHubRepositoryResponse getRepository(
            String owner,
            String repositoryName
    ) {
        return restClient.get()
                .uri(
                        "/repos/{owner}/{repo}",
                        owner,
                        repositoryName
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubRepositoryResponse.class);
    }

    public GitHubContentResponse[] getContents(
            String owner,
            String repositoryName,
            String path
    ) {
        String response;

        if (path == null || path.isBlank()) {
            response = restClient.get()
                    .uri(
                            "/repos/{owner}/{repo}/contents",
                            owner,
                            repositoryName
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } else {
            response = restClient.get()
                    .uri(
                            "/repos/{owner}/{repo}/contents/{path}",
                            owner,
                            repositoryName,
                            path
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.isArray()) {
                return objectMapper.treeToValue(
                        jsonNode,
                        GitHubContentResponse[].class
                );
            }

            GitHubContentResponse file =
                    objectMapper.treeToValue(
                            jsonNode,
                            GitHubContentResponse.class
                    );

            return new GitHubContentResponse[]{file};

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse GitHub contents response",
                    e
            );
        }
    }

    public GitHubTreeResponse getRepositoryTree(
            String owner,
            String repositoryName,
            String branch
    ) {
        return restClient.get()
                .uri(
                        "/repos/{owner}/{repo}/git/trees/{branch}?recursive=1",
                        owner,
                        repositoryName,
                        branch
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubTreeResponse.class);
    }

    public GitHubBlobResponse getBlob(
            String owner,
            String repositoryName,
            String fileSha
    ) {
        return restClient.get()
                .uri(
                        "/repos/{owner}/{repo}/git/blobs/{fileSha}",
                        owner,
                        repositoryName,
                        fileSha
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubBlobResponse.class);
    }
}