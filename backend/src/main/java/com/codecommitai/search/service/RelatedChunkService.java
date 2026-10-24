
package com.codecommitai.search.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.codechunk.repository.CodeChunkRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.repository.RepositoryFileRepository;
import com.codecommitai.search.dto.SearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RelatedChunkService {

    private static final int NEIGHBOR_DISTANCE = 1;

    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;

    public RelatedChunkService(
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository
    ) {
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> expand(
            UUID repositoryId,
            List<SearchResult> initialResults
    ) {
        if (repositoryId == null) {
            throw new IllegalArgumentException(
                    "Repository ID cannot be null"
            );
        }

        if (initialResults == null || initialResults.isEmpty()) {
            return List.of();
        }

        Map<String, SearchResult> expandedResults =
                new LinkedHashMap<>();

        // Keep the original search results and their ranking information.
        for (SearchResult result : initialResults) {
            if (result == null || result.filePath() == null
                    || result.chunkIndex() == null) {
                continue;
            }

            expandedResults.putIfAbsent(
                    buildKey(result.filePath(), result.chunkIndex()),
                    result
            );
        }

        // Find adjacent chunks for each original search result.
        for (SearchResult result : initialResults) {
            if (result == null || result.filePath() == null
                    || result.chunkIndex() == null) {
                continue;
            }

            RepositoryFile repositoryFile =
                    repositoryFileRepository
                            .findByRepositoryIdAndPath(
                                    repositoryId,
                                    result.filePath()
                            )
                            .orElse(null);

            if (repositoryFile == null) {
                continue;
            }

            int currentChunkIndex = result.chunkIndex();

            int firstChunkIndex = Math.max(
                    0,
                    currentChunkIndex - NEIGHBOR_DISTANCE
            );

            int lastChunkIndex =
                    currentChunkIndex + NEIGHBOR_DISTANCE;

            List<CodeChunk> neighboringChunks =
                    codeChunkRepository
                            .findAllByRepositoryFileIdAndChunkIndexBetweenOrderByChunkIndex(
                                    repositoryFile.getId(),
                                    firstChunkIndex,
                                    lastChunkIndex
                            );

            for (CodeChunk chunk : neighboringChunks) {
                SearchResult neighboringResult =
                        new SearchResult(
                                repositoryFile.getPath(),
                                repositoryFile.getFileName(),
                                chunk.getChunkIndex(),
                                chunk.getStartLine(),
                                chunk.getEndLine(),
                                chunk.getContent(),
                                0.0,
                                null,
                                null
                        );

                expandedResults.putIfAbsent(
                        buildKey(
                                repositoryFile.getPath(),
                                chunk.getChunkIndex()
                        ),
                        neighboringResult
                );
            }
        }

        return List.copyOf(expandedResults.values());
    }

    private String buildKey(
            String filePath,
            Integer chunkIndex
    ) {
        return filePath + "::" + chunkIndex;
    }
}

