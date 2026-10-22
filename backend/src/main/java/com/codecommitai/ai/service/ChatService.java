package com.codecommitai.ai.service;

import com.codecommitai.ai.dto.ChatResponse;
import com.codecommitai.ai.provider.TextGenerationProvider;
import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.HybridSearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final int SEARCH_LIMIT = 10;

    private final HybridSearchService hybridSearchService;
    private final CodebasePromptBuilder promptBuilder;
    private final TextGenerationProvider textGenerationProvider;

    public ChatService(
            HybridSearchService hybridSearchService,
            CodebasePromptBuilder promptBuilder,
            TextGenerationProvider textGenerationProvider
    ) {
        this.hybridSearchService = hybridSearchService;
        this.promptBuilder = promptBuilder;
        this.textGenerationProvider = textGenerationProvider;
    }

    public ChatResponse chat(
            UUID repositoryId,
            String question
    ) {
        if (repositoryId == null) {
            throw new IllegalArgumentException(
                    "Repository ID cannot be null"
            );
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question cannot be null or blank"
            );
        }

        List<SearchResult> searchResults =
                hybridSearchService.search(
                        repositoryId,
                        question.trim(),
                        SEARCH_LIMIT
                );

        PromptBuildResult promptBuildResult =
                promptBuilder.buildPrompt(
                        question.trim(),
                        searchResults
                );

        String answer =
                textGenerationProvider.generateText(
                        promptBuildResult.prompt()
                );

        return new ChatResponse(
                answer,
                promptBuildResult.sources()
        );
    }
}
