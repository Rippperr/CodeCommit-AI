
package com.codecommitai.embedding.provider;

import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_SECONDS = 40L;

    private static final Pattern RETRY_DELAY_PATTERN =
            Pattern.compile("retryDelay[\"']?\\s*:\\s*[\"']?(\\d+)s");

    private final Client client;
    private final String modelName;
    private final int dimensions;

    public GeminiEmbeddingProvider(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.embedding.model}") String modelName,
            @Value("${gemini.embedding.dimensions}") int dimensions
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured"
            );
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();

        this.modelName = modelName;
        this.dimensions = dimensions;
    }

    @Override
    public float[] generateEmbedding(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Text cannot be null or blank"
            );
        }

        List<float[]> embeddings =
                generateEmbeddings(List.of(text));

        return embeddings.get(0);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {

        validateInput(texts);

        if (texts.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "Embedding batch size cannot exceed "
                            + MAX_BATCH_SIZE
                            + " but received "
                            + texts.size()
            );
        }

        EmbedContentConfig config =
                EmbedContentConfig.builder()
                        .outputDimensionality(dimensions)
                        .build();

        ClientException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            try {

                EmbedContentResponse response =
                        client.models.embedContent(
                                modelName,
                                texts,
                                config
                        );

                return parseEmbeddings(
                        response,
                        texts.size()
                );

            } catch (ClientException e) {

                lastException = e;

                if (!isRetryableRateLimit(e)
                        || attempt == MAX_RETRIES) {
                    throw e;
                }

                long delaySeconds =
                        extractRetryDelaySeconds(e);

                System.out.println(
                        "Gemini rate limit reached. "
                                + "Retrying in "
                                + delaySeconds
                                + " seconds. Attempt "
                                + (attempt + 1)
                                + " of "
                                + MAX_RETRIES
                );

                sleep(delaySeconds);
            }
        }

        throw lastException;
    }

    private List<float[]> parseEmbeddings(
            EmbedContentResponse response,
            int expectedCount
    ) {

        List<com.google.genai.types.ContentEmbedding> embeddings =
                response.embeddings()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Gemini returned no embeddings"
                                )
                        );

        if (embeddings.size() != expectedCount) {
            throw new IllegalStateException(
                    "Gemini returned "
                            + embeddings.size()
                            + " embeddings for "
                            + expectedCount
                            + " inputs"
            );
        }

        List<float[]> result =
                new ArrayList<>(embeddings.size());

        for (com.google.genai.types.ContentEmbedding embedding :
                embeddings) {

            List<Float> values =
                    embedding.values()
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Gemini returned an embedding with no values"
                                    )
                            );

            if (values.size() != dimensions) {
                throw new IllegalStateException(
                        "Invalid Gemini embedding dimensions. Expected "
                                + dimensions
                                + " but received "
                                + values.size()
                );
            }

            float[] vector =
                    new float[values.size()];

            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i);
            }

            result.add(vector);
        }

        return result;
    }

    private void validateInput(List<String> texts) {

        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Texts cannot be null or empty"
            );
        }

        for (String text : texts) {

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Text cannot be null or blank"
                );
            }
        }
    }

    private boolean isRetryableRateLimit(
            ClientException exception
    ) {

        String message =
                exception.getMessage();

        return message != null
                && message.contains("429")
                && message.contains("RESOURCE_EXHAUSTED");
    }

    private long extractRetryDelaySeconds(
            ClientException exception
    ) {

        String message =
                exception.getMessage();

        if (message != null) {

            Matcher matcher =
                    RETRY_DELAY_PATTERN.matcher(message);

            if (matcher.find()) {

                try {
                    return Long.parseLong(
                            matcher.group(1)
                    );
                } catch (NumberFormatException ignored) {
                    // Use default delay below.
                }
            }
        }

        return DEFAULT_RETRY_DELAY_SECONDS;
    }

    private void sleep(long seconds) {

        try {

            Thread.sleep(
                    seconds * 1000L
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting to retry Gemini request",
                    e
            );
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }
}
