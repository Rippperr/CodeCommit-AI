package com.codecommitai.search.dto;

public record SearchResult(
        String filePath,
        String fileName,
        Integer chunkIndex,
        Integer startLine,
        Integer endLine,
        String content,
        Double score,
        Double keywordScore,
        Double semanticScore
) {
}