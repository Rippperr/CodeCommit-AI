
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

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        question,
                        List.of()
                );

        assertTrue(result.prompt().contains(question));
        assertNotNull(result.sources());
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void buildPrompt_shouldIncludeSourceAndCode() {

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

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain indexing",
                        List.of(searchResult)
                );

        String prompt = result.prompt();

        assertTrue(
                prompt.contains(
                        "src/main/java/IndexingService.java"
                )
        );

        assertTrue(prompt.contains("lines 10-25"));

        assertTrue(
                prompt.contains(
                        "public class IndexingService {}"
                )
        );

        assertEquals(1, result.sources().size());
        assertEquals(
                "src/main/java/IndexingService.java",
                result.sources().get(0).filePath()
        );
        assertEquals(10, result.sources().get(0).startLine());
        assertEquals(25, result.sources().get(0).endLine());
    }

    @Test
    void buildPrompt_shouldHandleEmptySearchResults() {

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain authentication",
                        List.of()
                );

        assertTrue(
                result.prompt().contains(
                        "No relevant code chunks were found"
                )
        );

        assertTrue(result.sources().isEmpty());
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

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain the code",
                        results
                );

        String prompt = result.prompt();

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

        assertEquals(8, result.sources().size());
    }

    @Test
    void buildPrompt_shouldPrioritizeProductionCodeOverTestCode() {

        SearchResult testResult = new SearchResult(
                "backend/src/test/java/HybridSearchServiceTest.java",
                "HybridSearchServiceTest.java",
                0,
                1,
                100,
                "TEST_CODE_MARKER",
                0.95,
                0.90,
                0.85
        );

        SearchResult productionResult = new SearchResult(
                "backend/src/main/java/com/codecommitai/search/service/HybridSearchService.java",
                "HybridSearchService.java",
                1,
                101,
                150,
                "PRODUCTION_CODE_MARKER",
                0.90,
                0.85,
                0.80
        );

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain HybridSearchService",
                        List.of(testResult, productionResult)
                );

        String prompt = result.prompt();

        int productionPosition =
                prompt.indexOf("PRODUCTION_CODE_MARKER");

        int testPosition =
                prompt.indexOf("TEST_CODE_MARKER");

        assertTrue(
                productionPosition >= 0,
                "Production code should be included"
        );

        assertTrue(
                testPosition >= 0,
                "Test code should be included"
        );

        assertTrue(
                productionPosition < testPosition,
                "Production code should appear before test code"
        );

        assertEquals(2, result.sources().size());

        assertEquals(
                productionResult.filePath(),
                result.sources().get(0).filePath()
        );

        assertEquals(
                testResult.filePath(),
                result.sources().get(1).filePath()
        );
    }

    @Test
    void buildPrompt_shouldRejectBlankQuestion() {

        assertThrows(
                IllegalArgumentException.class,
                () -> promptBuilder.buildPrompt("  ", List.of())
        );
    }
}
