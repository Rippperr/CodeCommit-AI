package com.codecommitai.codechunk.entity;

import com.codecommitai.repositoryfile.entity.RepositoryFile;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "code_chunks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_code_chunk_index",
                        columnNames = {
                                "repository_file_id",
                                "chunk_index"
                        }
                )
        }
)
public class CodeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "repository_file_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_code_chunks_repository_file"
            )
    )
    private RepositoryFile repositoryFile;

    @Column(
            name = "chunk_index",
            nullable = false
    )
    private Integer chunkIndex;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public RepositoryFile getRepositoryFile() {
        return repositoryFile;
    }

    public void setRepositoryFile(RepositoryFile repositoryFile) {
        this.repositoryFile = repositoryFile;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}