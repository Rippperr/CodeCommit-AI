package com.codecommitai.github.dto;

public record GitHubBlobResponse(
        String content,
        String encoding,
        String sha,
        Long size
) {
}