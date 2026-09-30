package com.codecommitai.embedding.provider;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

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

        EmbedContentConfig config =
                EmbedContentConfig.builder()
                        .outputDimensionality(dimensions)
                        .build();

        EmbedContentResponse response =
                client.models.embedContent(
                        modelName,
                        Content.fromParts(
                                com.google.genai.types.Part.fromText(text)
                        ),
                        config
                );

        List<Float> values =
                response.embeddings()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Gemini returned no embeddings"
                                ))
                        .get(0)
                        .values()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Gemini returned an embedding with no values"
                                ));

        if (values.size() != dimensions) {
            throw new IllegalStateException(
                    "Invalid Gemini embedding dimensions. Expected "
                            + dimensions
                            + " but received "
                            + values.size()
            );
        }

        float[] result = new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }

        return result;
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