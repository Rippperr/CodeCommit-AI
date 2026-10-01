package com.codecommitai.embedding.provider;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiEmbeddingProviderTest {

    @Test
    void shouldExposeModelNameAndDimensions() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        GeminiEmbeddingProvider provider =
                new GeminiEmbeddingProvider(
                        client,
                        "gemini-embedding-2",
                        1536
                );

        assertEquals(
                "gemini-embedding-2",
                provider.getModelName()
        );

        assertEquals(
                1536,
                provider.getDimensions()
        );
    }

    @Test
    void constructorShouldRejectNullClient() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new GeminiEmbeddingProvider(
                                (Client) null,
                                "gemini-embedding-2",
                                1536
                        )
                );

        assertEquals(
                "Gemini client cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void constructorShouldRejectBlankModelName() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new GeminiEmbeddingProvider(
                                client,
                                "   ",
                                1536
                        )
                );

        assertEquals(
                "Gemini model name cannot be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void constructorShouldRejectInvalidDimensions() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new GeminiEmbeddingProvider(
                                client,
                                "gemini-embedding-2",
                                0
                        )
                );

        assertEquals(
                "Embedding dimensions must be greater than zero",
                exception.getMessage()
        );
    }

    @Test
    void generateEmbeddingShouldRejectBlankText() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        GeminiEmbeddingProvider provider =
                new GeminiEmbeddingProvider(
                        client,
                        "gemini-embedding-2",
                        1536
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.generateEmbedding("   ")
                );

        assertEquals(
                "Text cannot be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void generateEmbeddingsShouldRejectEmptyInput() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        GeminiEmbeddingProvider provider =
                new GeminiEmbeddingProvider(
                        client,
                        "gemini-embedding-2",
                        1536
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.generateEmbeddings(List.of())
                );

        assertEquals(
                "Texts cannot be null or empty",
                exception.getMessage()
        );
    }

    @Test
    void generateEmbeddingsShouldRejectBlankTextInsideBatch() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        GeminiEmbeddingProvider provider =
                new GeminiEmbeddingProvider(
                        client,
                        "gemini-embedding-2",
                        1536
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.generateEmbeddings(
                                List.of(
                                        "valid text",
                                        "   "
                                )
                        )
                );

        assertEquals(
                "Text cannot be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void generateEmbeddingsShouldRejectBatchLargerThan100() {
        Client client = Client.builder()
                .apiKey("test-key")
                .build();

        GeminiEmbeddingProvider provider =
                new GeminiEmbeddingProvider(
                        client,
                        "gemini-embedding-2",
                        1536
                );

        List<String> texts =
                IntStream
                        .range(0, 101)
                        .mapToObj(i -> "text-" + i)
                        .toList();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.generateEmbeddings(texts)
                );

        assertTrue(
                exception.getMessage()
                        .contains("cannot exceed 100")
        );
    }

    @Test
    void contentEmbeddingShouldExposeExpectedValues() {
        List<Float> values =
                List.of(
                        0.1f,
                        0.2f,
                        0.3f
                );

        ContentEmbedding embedding =
                ContentEmbedding.builder()
                        .values(values)
                        .build();

        assertTrue(
                embedding.values().isPresent()
        );

        assertEquals(
                values,
                embedding.values().get()
        );
    }

    @Test
    void embedContentResponseShouldExposeEmbeddings() {
        ContentEmbedding embedding =
                ContentEmbedding.builder()
                        .values(
                                List.of(
                                        0.1f,
                                        0.2f,
                                        0.3f
                                )
                        )
                        .build();

        EmbedContentResponse response =
                EmbedContentResponse.builder()
                        .embeddings(
                                List.of(embedding)
                        )
                        .build();

        assertTrue(
                response.embeddings().isPresent()
        );

        assertEquals(
                1,
                response.embeddings().get().size()
        );
    }
}