package com.codecommitai.search.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class KeywordSearchRepositoryTest {

    @Autowired
    private KeywordSearchRepository keywordSearchRepository;

    @Test
    void search_shouldReturnIndexedCodeChunks() {
        UUID repositoryId =
                UUID.fromString("a2d08518-be93-4ba0-8931-76d50eef3fd3");

        List<KeywordSearchProjection> results =
                keywordSearchRepository.search(
                        repositoryId,
                        "Repository",
                        "Repository",
                        10
                );

        assertFalse(results.isEmpty());

        KeywordSearchProjection firstResult = results.get(0);

        assertNotNull(firstResult.getFilePath());
        assertNotNull(firstResult.getFileName());
        assertNotNull(firstResult.getChunkIndex());
        assertNotNull(firstResult.getStartLine());
        assertNotNull(firstResult.getEndLine());
        assertNotNull(firstResult.getContent());
        assertNotNull(firstResult.getRelevanceScore());

        assertTrue(firstResult.getRelevanceScore() > 0);
    }
}