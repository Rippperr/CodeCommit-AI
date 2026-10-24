
package com.codecommitai.search.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.codechunk.repository.CodeChunkRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.repository.RepositoryFileRepository;
import com.codecommitai.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatedChunkServiceTest {

    @Mock
    private RepositoryFileRepository repositoryFileRepository;

    @Mock
    private CodeChunkRepository codeChunkRepository;

    private RelatedChunkService relatedChunkService;

    @BeforeEach
    void setUp() {
        relatedChunkService = new RelatedChunkService(
                repositoryFileRepository,
                codeChunkRepository
        );
    }

    @Test
    void expand_shouldIncludePreviousCurrentAndNextChunks() {
        UUID repositoryId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        RepositoryFile file = createFile(
                fileId,
                "src/main/java/Example.java",
                "Example.java"
        );

        SearchResult original = createSearchResult(
                "src/main/java/Example.java",
                "Example.java",
                2,
                201,
                300,
                "Original matching code",
                0.95
        );

        List<CodeChunk> neighboringChunks = List.of(
                createChunk(file, 1, 101, 200, "Previous chunk"),
                createChunk(file, 2, 201, 300, "Original matching code"),
                createChunk(file, 3, 301, 350, "Next chunk")
        );

        when(repositoryFileRepository.findByRepositoryIdAndPath(
                repositoryId,
                original.filePath()
        )).thenReturn(Optional.of(file));

        when(codeChunkRepository
                .findAllByRepositoryFileIdAndChunkIndexBetweenOrderByChunkIndex(
                        fileId,
                        1,
                        3
                )).thenReturn(neighboringChunks);

        List<SearchResult> results = relatedChunkService.expand(
                repositoryId,
                List.of(original)
        );

        assertEquals(3, results.size());

        // Original results are retained first to preserve their ranking priority.
        assertEquals(
                List.of(2, 1, 3),
                results.stream()
                        .map(SearchResult::chunkIndex)
                        .toList()
        );

        assertEquals(0.95, results.get(0).score());
        assertEquals("Original matching code", results.get(0).content());
        assertEquals("Previous chunk", results.get(1).content());
        assertEquals("Next chunk", results.get(2).content());

        verify(repositoryFileRepository).findByRepositoryIdAndPath(
                repositoryId,
                original.filePath()
        );
    }

    @Test
    void expand_shouldStartAtChunkZeroForFirstChunk() {
        UUID repositoryId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        RepositoryFile file = createFile(
                fileId,
                "Example.java",
                "Example.java"
        );

        SearchResult original = createSearchResult(
                "Example.java",
                "Example.java",
                0,
                1,
                100,
                "First chunk",
                0.9
        );

        List<CodeChunk> chunks = List.of(
                createChunk(file, 0, 1, 100, "First chunk"),
                createChunk(file, 1, 91, 190, "Second chunk")
        );

        when(repositoryFileRepository.findByRepositoryIdAndPath(
                repositoryId,
                "Example.java"
        )).thenReturn(Optional.of(file));

        when(codeChunkRepository
                .findAllByRepositoryFileIdAndChunkIndexBetweenOrderByChunkIndex(
                        fileId,
                        0,
                        1
                )).thenReturn(chunks);

        List<SearchResult> results = relatedChunkService.expand(
                repositoryId,
                List.of(original)
        );

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).chunkIndex());
        assertEquals(1, results.get(1).chunkIndex());
    }

    @Test
    void expand_shouldNotDuplicateChunksWhenNeighborRangesOverlap() {
        UUID repositoryId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        RepositoryFile file = createFile(
                fileId,
                "Example.java",
                "Example.java"
        );

        SearchResult first = createSearchResult(
                "Example.java",
                "Example.java",
                2,
                201,
                300,
                "Chunk two",
                0.9
        );

        SearchResult second = createSearchResult(
                "Example.java",
                "Example.java",
                3,
                291,
                390,
                "Chunk three",
                0.8
        );

        when(repositoryFileRepository.findByRepositoryIdAndPath(
                repositoryId,
                "Example.java"
        )).thenReturn(Optional.of(file));

        when(codeChunkRepository
                .findAllByRepositoryFileIdAndChunkIndexBetweenOrderByChunkIndex(
                        fileId,
                        1,
                        3
                )).thenReturn(List.of(
                        createChunk(file, 1, 101, 200, "Chunk one"),
                        createChunk(file, 2, 201, 300, "Chunk two"),
                        createChunk(file, 3, 291, 390, "Chunk three")
                ));

        when(codeChunkRepository
                .findAllByRepositoryFileIdAndChunkIndexBetweenOrderByChunkIndex(
                        fileId,
                        2,
                        4
                )).thenReturn(List.of(
                        createChunk(file, 2, 201, 300, "Chunk two"),
                        createChunk(file, 3, 291, 390, "Chunk three"),
                        createChunk(file, 4, 381, 450, "Chunk four")
                ));

        List<SearchResult> results = relatedChunkService.expand(
                repositoryId,
                List.of(first, second)
        );

        assertEquals(4, results.size());

        assertEquals(
                List.of(2, 3, 1, 4),
                results.stream()
                        .map(SearchResult::chunkIndex)
                        .toList()
        );

        assertEquals(0.9, results.get(0).score());
        assertEquals(0.8, results.get(1).score());
    }

    @Test
    void expand_shouldKeepOriginalResultWhenFileCannotBeResolved() {
        UUID repositoryId = UUID.randomUUID();

        SearchResult original = createSearchResult(
                "Missing.java",
                "Missing.java",
                0,
                1,
                20,
                "Original code",
                0.75
        );

        when(repositoryFileRepository.findByRepositoryIdAndPath(
                repositoryId,
                "Missing.java"
        )).thenReturn(Optional.empty());

        List<SearchResult> results = relatedChunkService.expand(
                repositoryId,
                List.of(original)
        );

        assertEquals(1, results.size());
        assertSame(original, results.get(0));

        verifyNoInteractions(codeChunkRepository);
    }

    @Test
    void expand_shouldReturnEmptyListForEmptyResults() {
        List<SearchResult> results = relatedChunkService.expand(
                UUID.randomUUID(),
                List.of()
        );

        assertTrue(results.isEmpty());

        verifyNoInteractions(
                repositoryFileRepository,
                codeChunkRepository
        );
    }

    @Test
    void expand_shouldRejectNullRepositoryId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> relatedChunkService.expand(null, List.of())
        );

        verifyNoInteractions(
                repositoryFileRepository,
                codeChunkRepository
        );
    }

    private RepositoryFile createFile(
            UUID fileId,
            String path,
            String fileName
    ) {
        RepositoryFile file = new RepositoryFile();
        file.setId(fileId);
        file.setPath(path);
        file.setFileName(fileName);
        return file;
    }

    private CodeChunk createChunk(
            RepositoryFile file,
            int chunkIndex,
            int startLine,
            int endLine,
            String content
    ) {
        CodeChunk chunk = new CodeChunk();
        chunk.setRepositoryFile(file);
        chunk.setChunkIndex(chunkIndex);
        chunk.setStartLine(startLine);
        chunk.setEndLine(endLine);
        chunk.setContent(content);
        return chunk;
    }

    private SearchResult createSearchResult(
            String filePath,
            String fileName,
            int chunkIndex,
            int startLine,
            int endLine,
            String content,
            double score
    ) {
        return new SearchResult(
                filePath,
                fileName,
                chunkIndex,
                startLine,
                endLine,
                content,
                score,
                null,
                null
        );
    }
}
