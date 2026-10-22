
package com.codecommitai.ai.service;

import com.codecommitai.ai.dto.ChatResponse;
import com.codecommitai.ai.dto.SourceReference;
import com.codecommitai.ai.provider.TextGenerationProvider;
import com.codecommitai.search.dto.SearchResult;
import com.codecommitai.search.service.HybridSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private HybridSearchService hybridSearchService;

    @Mock
    private CodebasePromptBuilder promptBuilder;

    @Mock
    private TextGenerationProvider textGenerationProvider;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                hybridSearchService,
                promptBuilder,
                textGenerationProvider
        );
    }

    @Test
    void chat_shouldRetrieveCodeGenerateAnswerAndReturnSources() {

        UUID repositoryId = UUID.randomUUID();

        String question = "Explain indexing";

        String prompt = "Prepared RAG prompt";

        String answer =
                "Indexing processes repository files into searchable chunks.";

        SearchResult searchResult = new SearchResult(
                "src/main/java/IndexingService.java",
                "IndexingService.java",
                0,
                10,
                25,
                "public class IndexingService {}",
                0.95,
                0.90,
                0.85
        );

        List<SearchResult> searchResults =
                List.of(searchResult);

        List<SourceReference> sources =
                List.of(
                        new SourceReference(
                                "src/main/java/IndexingService.java",
                                10,
                                25
                        )
                );

        PromptBuildResult promptBuildResult =
                new PromptBuildResult(prompt, sources);

        when(hybridSearchService.search(
                repositoryId,
                question,
                10
        )).thenReturn(searchResults);

        when(promptBuilder.buildPrompt(
                question,
                searchResults
        )).thenReturn(promptBuildResult);

        when(textGenerationProvider.generateText(prompt))
                .thenReturn(answer);

        ChatResponse response =
                chatService.chat(
                        repositoryId,
                        question
                );

        assertEquals(answer, response.answer());

        assertEquals(1, response.sources().size());

        assertEquals(
                "src/main/java/IndexingService.java",
                response.sources().get(0).filePath()
        );

        assertEquals(
                10,
                response.sources().get(0).startLine()
        );

        assertEquals(
                25,
                response.sources().get(0).endLine()
        );

        verify(hybridSearchService).search(
                repositoryId,
                question,
                10
        );

        verify(promptBuilder).buildPrompt(
                question,
                searchResults
        );

        verify(textGenerationProvider).generateText(prompt);
    }

    @Test
    void chat_shouldReturnEmptySourcesWhenNoCodeIsFound() {

        UUID repositoryId = UUID.randomUUID();

        String question = "Explain an unknown feature";

        String prompt = "Prompt with no retrieved code";

        String answer =
                "I could not find relevant code to answer this confidently.";

        PromptBuildResult promptBuildResult =
                new PromptBuildResult(prompt, List.of());

        when(hybridSearchService.search(
                repositoryId,
                question,
                10
        )).thenReturn(List.of());

        when(promptBuilder.buildPrompt(
                question,
                List.of()
        )).thenReturn(promptBuildResult);

        when(textGenerationProvider.generateText(prompt))
                .thenReturn(answer);

        ChatResponse response =
                chatService.chat(
                        repositoryId,
                        question
                );

        assertEquals(answer, response.answer());

        assertTrue(response.sources().isEmpty());

        verify(textGenerationProvider).generateText(prompt);
    }

    @Test
    void chat_shouldRejectNullRepositoryId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> chatService.chat(
                        null,
                        "Explain indexing"
                )
        );

        verifyNoInteractions(
                hybridSearchService,
                promptBuilder,
                textGenerationProvider
        );
    }

    @Test
    void chat_shouldRejectBlankQuestion() {

        assertThrows(
                IllegalArgumentException.class,
                () -> chatService.chat(
                        UUID.randomUUID(),
                        "   "
                )
        );

        verifyNoInteractions(
                hybridSearchService,
                promptBuilder,
                textGenerationProvider
        );
    }
}
