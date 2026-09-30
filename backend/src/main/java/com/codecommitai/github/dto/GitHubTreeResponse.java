package com.codecommitai.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubTreeResponse(
        String sha,
        TreeEntry[] tree,
        boolean truncated
) {

    public record TreeEntry(
            String path,
            String mode,
            String type,
            Long size,
            String sha,
            String url
    ) {}
}