package com.codecommitai.repository.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RepositoryResponse(

        UUID id,

        Long githubId,

        String owner,

        String name,

        String fullName,

        String defaultBranch,

        String description,

        String htmlUrl,

        boolean privateRepository,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}