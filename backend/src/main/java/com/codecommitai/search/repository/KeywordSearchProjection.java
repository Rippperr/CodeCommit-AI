package com.codecommitai.search.repository;

public interface KeywordSearchProjection {

    String getFilePath();

    String getFileName();

    Integer getChunkIndex();

    Integer getStartLine();

    Integer getEndLine();

    String getContent();

    Double getRelevanceScore();
}