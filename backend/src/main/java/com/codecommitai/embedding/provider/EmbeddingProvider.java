package com.codecommitai.embedding.provider;

public interface EmbeddingProvider {

    float[] generateEmbedding(String text);

    String getModelName();

    int getDimensions();
}