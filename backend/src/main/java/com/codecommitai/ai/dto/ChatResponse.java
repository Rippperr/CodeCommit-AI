package com.codecommitai.ai.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<SourceReference> sources
) {
}