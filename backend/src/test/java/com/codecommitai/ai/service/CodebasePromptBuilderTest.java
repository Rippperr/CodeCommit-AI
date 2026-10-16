
package com.codecommitai.ai.service;

import com.codecommitai.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodebasePromptBuilderTest {

    private CodebasePromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new CodebasePromptBuilder();
    }

    @Test
    void buildPrompt_shouldIncludeQuestion() {

        String question =
                "How does repository indexing work?";

        String prompt =
                promptBuilder.buildPrompt(
                        question,
                        List.of()
                );

        assertTrue(prompt.contains(question));
    }

    @Test
    void buildPrompt_shouldIncludeSourceAndCode() {

        SearchResult result = new SearchResult(
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

        String prompt =
                promptBuilder.buildPrompt(
                        "Explain indexing",
                        List.of(result)
                );

        assertTrue(
                prompt.contains(
                        "src/main/java/IndexingService.java"
                )
        );

        assertTrue(
                prompt.contains("lines 10-25")
        );

        assertTrue(
                prompt.contains(
                        "public class IndexingService {}"
                )
        );
    }

    @Test
    void buildPrompt_shouldHandleEmptySearchResults() {

        String prompt =
                promptBuilder.buildPrompt(
                        "Explain authentication",
                        List.of()
                );

        assertTrue(
                prompt.contains(
                        "No relevant code chunks were found"
                )
        );
    }

    @Test
    void buildPrompt_shouldLimitNumberOfChunks() {

        List<SearchResult> results =
                new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            results.add(
                    new SearchResult(
                            "src/File" + i + ".java",
                            "File" + i + ".java",
                            i,
                            1,
                            10,
                            "UNIQUE_CODE_CHUNK_" + i,
                            0.9,
                            0.8,
                            0.7
                    )
            );
        }

        String prompt =
                promptBuilder.buildPrompt(
                        "Explain the code",
                        results
                );

        for (int i = 0; i < 8; i++) {
            assertTrue(
                    prompt.contains(
                            "UNIQUE_CODE_CHUNK_" + i
                    )
            );
        }

        assertFalse(
                prompt.contains("UNIQUE_CODE_CHUNK_8")
        );

        assertFalse(
                prompt.contains("UNIQUE_CODE_CHUNK_9")
        );
    }
}
