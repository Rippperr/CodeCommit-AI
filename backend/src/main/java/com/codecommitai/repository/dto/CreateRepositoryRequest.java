package com.codecommitai.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRepositoryRequest(

        @NotNull(message = "GitHub repository ID is required")
        Long githubId,

        @NotBlank(message = "Owner is required")
        @Size(max = 255, message = "Owner must not exceed 255 characters")
        String owner,

        @NotBlank(message = "Repository name is required")
        @Size(max = 255, message = "Repository name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Full repository name is required")
        @Size(max = 511, message = "Full name must not exceed 511 characters")
        String fullName,

        @Size(max = 255, message = "Default branch must not exceed 255 characters")
        String defaultBranch,

        String description,

        String htmlUrl,

        boolean privateRepository
) {
}