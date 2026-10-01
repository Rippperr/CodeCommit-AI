package com.codecommitai.embedding.provider;

import java.util.List;

public interface EmbeddingProvider {

    float[] generateEmbedding(String text);

    List<float[]> generateEmbeddings(List<String> texts);

    String getModelName();

    int getDimensions();
}