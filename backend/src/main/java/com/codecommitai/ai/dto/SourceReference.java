package com.codecommitai.ai.dto;

public record SourceReference(
        String filePath,
        Integer startLine,
        Integer endLine
) {
}