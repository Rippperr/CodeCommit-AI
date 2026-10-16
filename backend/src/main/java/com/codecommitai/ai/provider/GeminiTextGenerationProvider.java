
package com.codecommitai.ai.provider;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiTextGenerationProvider
        implements TextGenerationProvider {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GeminiTextGenerationProvider.class
            );

    private static final int GEMINI_TIMEOUT_MS = 90_000;

    private final Client client;
    private final String modelName;

    @Autowired
    public GeminiTextGenerationProvider(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.chat.model}") String modelName
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured"
            );
        }

        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException(
                    "Gemini chat model is not configured"
            );
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(
                        HttpOptions.builder()
                                .timeout(GEMINI_TIMEOUT_MS)
                                .build()
                )
                .build();

        this.modelName = modelName;
    }

    @Override
    public String generateText(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Prompt cannot be null or blank"
            );
        }

        log.info(
                "Starting Gemini generation. Model: {}, prompt length: {}",
                modelName,
                prompt.length()
        );

        try {
            GenerateContentConfig config =
                    GenerateContentConfig.builder().build();

            log.info("Sending request to Gemini.");

            GenerateContentResponse response =
                    client.models.generateContent(
                            modelName,
                            prompt,
                            config
                    );

            String generatedText = response.text();

            if (generatedText == null || generatedText.isBlank()) {
                throw new IllegalStateException(
                        "Gemini returned an empty response"
                );
            }

            log.info(
                    "Gemini generation completed. Response length: {}",
                    generatedText.length()
            );

            return generatedText.trim();

        } catch (RuntimeException exception) {
            log.error(
                    "Gemini generation failed. Model: {}",
                    modelName,
                    exception
            );
            throw exception;
        }
    }
}
