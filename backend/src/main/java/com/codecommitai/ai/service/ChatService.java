
package com.codecommitai.ai.service;

import com.codecommitai.ai.dto.ChatResponse;
import com.codecommitai.ai.provider.TextGenerationProvider;
import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.HybridSearchService;
import com.codecommitai.search.service.RelatedChunkService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final int SEARCH_LIMIT = 10;

    private final HybridSearchService hybridSearchService;
    private final RelatedChunkService relatedChunkService;
    private final CodebasePromptBuilder promptBuilder;
    private final TextGenerationProvider textGenerationProvider;

    public ChatService(
            HybridSearchService hybridSearchService,
            RelatedChunkService relatedChunkService,
            CodebasePromptBuilder promptBuilder,
            TextGenerationProvider textGenerationProvider
    ) {
        this.hybridSearchService = hybridSearchService;
        this.relatedChunkService = relatedChunkService;
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

        String normalizedQuestion = question.trim();

        List<SearchResult> searchResults =
                hybridSearchService.search(
                        repositoryId,
                        normalizedQuestion,
                        SEARCH_LIMIT
                );

        List<SearchResult> expandedResults =
                relatedChunkService.expand(
                        repositoryId,
                        searchResults
                );

        PromptBuildResult promptBuildResult =
                promptBuilder.buildPrompt(
                        normalizedQuestion,
                        expandedResults
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
