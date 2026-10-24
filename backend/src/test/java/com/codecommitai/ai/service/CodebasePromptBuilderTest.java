
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
        String question = "How does repository indexing work?";

        PromptBuildResult result =
                promptBuilder.buildPrompt(question, List.of());

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

        assertTrue(prompt.contains("src/main/java/IndexingService.java"));
        assertTrue(prompt.contains("lines 10-25"));
        assertTrue(prompt.contains("public class IndexingService {}"));

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
    void buildPrompt_shouldLimitNumberOfChunksToTwelve() {
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            results.add(new SearchResult(
                    "src/File" + i + ".java",
                    "File" + i + ".java",
                    i,
                    1,
                    10,
                    "UNIQUE_CODE_CHUNK_" + i,
                    0.9,
                    0.8,
                    0.7
            ));
        }

        PromptBuildResult result =
                promptBuilder.buildPrompt("Explain the code", results);

        String prompt = result.prompt();

        for (int i = 0; i < 12; i++) {
            assertTrue(
                    prompt.contains("UNIQUE_CODE_CHUNK_" + i),
                    "Expected chunk " + i + " to be included"
            );
        }

        assertFalse(prompt.contains("UNIQUE_CODE_CHUNK_12"));
        assertFalse(prompt.contains("UNIQUE_CODE_CHUNK_13"));
        assertFalse(prompt.contains("UNIQUE_CODE_CHUNK_14"));

        assertEquals(12, result.sources().size());
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
        int testPosition = prompt.indexOf("TEST_CODE_MARKER");

        assertTrue(productionPosition >= 0);
        assertTrue(testPosition >= 0);
        assertTrue(productionPosition < testPosition);

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
    void buildPrompt_shouldLimitChunksFromSameFile() {
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            results.add(new SearchResult(
                    "src/main/java/HybridSearchService.java",
                    "HybridSearchService.java",
                    i,
                    i * 90 + 1,
                    i * 90 + 100,
                    "HYBRID_CHUNK_" + i,
                    0.95 - (i * 0.01),
                    0.90,
                    0.85
            ));
        }

        results.add(new SearchResult(
                "src/main/java/SearchRankingService.java",
                "SearchRankingService.java",
                0,
                1,
                100,
                "RANKING_SERVICE_CHUNK",
                0.80,
                0.75,
                0.70
        ));

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain HybridSearchService and ranking",
                        results
                );

        String prompt = result.prompt();

        assertTrue(prompt.contains("HYBRID_CHUNK_0"));
        assertTrue(prompt.contains("HYBRID_CHUNK_1"));
        assertTrue(prompt.contains("HYBRID_CHUNK_2"));
        assertFalse(prompt.contains("HYBRID_CHUNK_3"));
        assertFalse(prompt.contains("HYBRID_CHUNK_4"));

        assertTrue(
                prompt.contains("RANKING_SERVICE_CHUNK"),
                "Another relevant production file should not be crowded out"
        );
    }

    @Test
    void buildPrompt_shouldNotRepeatIdenticalOverlappingContent() {
        String sharedContent =
                "public void mergeResults() { return; }";

        SearchResult firstChunk = new SearchResult(
                "src/main/java/HybridSearchService.java",
                "HybridSearchService.java",
                0,
                1,
                100,
                sharedContent,
                0.95,
                0.90,
                0.85
        );

        SearchResult secondChunk = new SearchResult(
                "src/main/java/HybridSearchService.java",
                "HybridSearchService.java",
                1,
                91,
                190,
                sharedContent,
                0.90,
                0.85,
                0.80
        );

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain mergeResults",
                        List.of(firstChunk, secondChunk)
                );

        String prompt = result.prompt();
        int firstOccurrence = prompt.indexOf(sharedContent);

        assertTrue(firstOccurrence >= 0);

        assertEquals(
                -1,
                prompt.indexOf(
                        sharedContent,
                        firstOccurrence + sharedContent.length()
                ),
                "Identical code content should not be repeated in the prompt"
        );

        assertEquals(2, result.sources().size());
    }

    @Test
    void buildPrompt_shouldRemoveOverlappingLinesFromSameFile() {
        String firstContent = """
                LINE_1
                LINE_2
                LINE_3
                LINE_4
                LINE_5""";

        String secondContent = """
                LINE_4
                LINE_5
                LINE_6
                LINE_7
                LINE_8""";

        SearchResult firstChunk = new SearchResult(
                "src/main/java/ExampleService.java",
                "ExampleService.java",
                0,
                1,
                5,
                firstContent,
                0.95,
                0.90,
                0.85
        );

        SearchResult secondChunk = new SearchResult(
                "src/main/java/ExampleService.java",
                "ExampleService.java",
                1,
                4,
                8,
                secondContent,
                0.90,
                0.85,
                0.80
        );

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain ExampleService",
                        List.of(firstChunk, secondChunk)
                );

        String prompt = result.prompt();

        assertTrue(prompt.contains("LINE_1"));
        assertTrue(prompt.contains("LINE_8"));

        assertEquals(1, countCompleteLineOccurrences(prompt, "LINE_4"));
        assertEquals(1, countCompleteLineOccurrences(prompt, "LINE_5"));
        assertEquals(2, result.sources().size());
    }

    @Test
    void buildPrompt_shouldRemoveOverlappingLinesAcrossThreeChunks() {
        String firstContent = """
                LINE_1
                LINE_2
                LINE_3
                LINE_4
                LINE_5""";

        String secondContent = """
                LINE_4
                LINE_5
                LINE_6
                LINE_7
                LINE_8""";

        String thirdContent = """
                LINE_7
                LINE_8
                LINE_9
                LINE_10
                LINE_11""";

        SearchResult firstChunk = new SearchResult(
                "src/main/java/ExampleService.java",
                "ExampleService.java",
                0,
                1,
                5,
                firstContent,
                0.95,
                0.90,
                0.85
        );

        SearchResult secondChunk = new SearchResult(
                "src/main/java/ExampleService.java",
                "ExampleService.java",
                1,
                4,
                8,
                secondContent,
                0.90,
                0.85,
                0.80
        );

        SearchResult thirdChunk = new SearchResult(
                "src/main/java/ExampleService.java",
                "ExampleService.java",
                2,
                7,
                11,
                thirdContent,
                0.85,
                0.80,
                0.75
        );

        PromptBuildResult result =
                promptBuilder.buildPrompt(
                        "Explain ExampleService",
                        List.of(firstChunk, secondChunk, thirdChunk)
                );

        String prompt = result.prompt();

        for (int line = 1; line <= 11; line++) {
            String marker = "LINE_" + line;

            assertEquals(
                    1,
                    countCompleteLineOccurrences(prompt, marker),
                    marker + " should appear exactly once as a complete line"
            );
        }

        assertEquals(
                3,
                result.sources().size(),
                "All three source references should be preserved"
        );
    }

    private long countCompleteLineOccurrences(String text, String target) {
        return text.lines()
                .filter(line -> line.equals(target))
                .count();
    }

    @Test
    void buildPrompt_shouldRejectBlankQuestion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> promptBuilder.buildPrompt("  ", List.of())
        );
    }
}
