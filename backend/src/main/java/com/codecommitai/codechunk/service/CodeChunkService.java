package com.codecommitai.codechunk.service;

import com.codecommitai.codechunk.entity.CodeChunk;
import com.codecommitai.codechunk.repository.CodeChunkRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.repository.RepositoryFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CodeChunkService {

    private static final int CHUNK_SIZE = 100;
    private static final int CHUNK_OVERLAP = 10;

    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;

    public CodeChunkService(
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository
    ) {
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
    }

    @Transactional
    public int chunkFile(UUID repositoryFileId) {
        RepositoryFile repositoryFile =
                repositoryFileRepository.findById(repositoryFileId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Repository file not found: " + repositoryFileId
                                ));

        if (repositoryFile.getContent() == null
                || repositoryFile.getContent().isBlank()) {
            throw new IllegalStateException(
                    "Repository file has no content: " + repositoryFile.getPath()
            );
        }

        // Remove previously generated chunks so the file can be re-indexed safely.
        codeChunkRepository.deleteAllByRepositoryFileId(repositoryFileId);

        // Force Hibernate to execute the DELETE before inserting chunk_index = 0 again.
        codeChunkRepository.flush();

        List<String> lines = repositoryFile.getContent().lines().toList();

        if (lines.isEmpty()) {
            return 0;
        }

        List<CodeChunk> chunks = new ArrayList<>();

        int chunkIndex = 0;
        int startLine = 0;

        while (startLine < lines.size()) {
            int endLine = Math.min(
                    startLine + CHUNK_SIZE,
                    lines.size()
            );

            List<String> chunkLines =
                    lines.subList(startLine, endLine);

            String chunkContent = String.join(
                    System.lineSeparator(),
                    chunkLines
            );

            CodeChunk codeChunk = new CodeChunk();

            codeChunk.setRepositoryFile(repositoryFile);
            codeChunk.setChunkIndex(chunkIndex);
            codeChunk.setContent(chunkContent);
            codeChunk.setStartLine(startLine + 1);
            codeChunk.setEndLine(endLine);
            codeChunk.setTokenCount(estimateTokenCount(chunkContent));

            chunks.add(codeChunk);

            chunkIndex++;

            if (endLine >= lines.size()) {
                break;
            }

            startLine = endLine - CHUNK_OVERLAP;

            if (startLine < 0) {
                startLine = 0;
            }
        }

        codeChunkRepository.saveAll(chunks);
        codeChunkRepository.flush();

        return chunks.size();
    }

    @Transactional
    public int chunkRepository(UUID repositoryId) {
        List<RepositoryFile> files =
                repositoryFileRepository.findAllByRepositoryId(repositoryId);

        int chunksCreated = 0;

        for (RepositoryFile file : files) {
            chunksCreated += chunkFile(file.getId());
        }

        return chunksCreated;
    }

    @Transactional(readOnly = true)
    public List<CodeChunk> getChunksForFile(UUID repositoryFileId) {
        return codeChunkRepository
                .findAllByRepositoryFileIdOrderByChunkIndex(repositoryFileId);
    }

    @Transactional(readOnly = true)
    public List<CodeChunk> getChunksForRepository(UUID repositoryId) {
        return codeChunkRepository
                .findAllByRepositoryFileRepositoryId(repositoryId);
    }

    private int estimateTokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        return content.trim().split("\\s+").length;
    }
}