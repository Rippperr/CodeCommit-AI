package com.codecommitai.embedding.dto;

public record VectorSearchResult(
        String filePath,
        String fileName,
        Integer chunkIndex,
        Integer startLine,
        Integer endLine,
        String content,
        Double cosineDistance
) {
}