package com.codecommitai.github.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportRepositoryRequest(

        @NotBlank(message = "Repository owner is required")
        String owner,

        @NotBlank(message = "Repository name is required")
        String repositoryName
) {
}