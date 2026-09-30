package com.codecommitai.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryResponse(

        Long id,

        String name,

        @JsonProperty("full_name")
        String fullName,

        String description,

        @JsonProperty("html_url")
        String htmlUrl,

        @JsonProperty("default_branch")
        String defaultBranch,

        @JsonProperty("private")
        boolean privateRepository,

        Owner owner
) {

    public record Owner(
            String login
    ) {
    }
}