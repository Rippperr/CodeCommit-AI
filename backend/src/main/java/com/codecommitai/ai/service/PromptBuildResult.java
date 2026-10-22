package com.codecommitai.ai.service;

import com.codecommitai.ai.dto.SourceReference;

import java.util.List;

public record PromptBuildResult(
        String prompt,
        List<SourceReference> sources
) {
    public PromptBuildResult {
        sources = List.copyOf(sources);
    }
}
