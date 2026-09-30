package com.codecommitai.indexing.entity;

import com.codecommitai.repository.entity.Repository;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "indexing_jobs")
public class IndexingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "repository_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_indexing_jobs_repository"
            )
    )
    private Repository repository;

    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private String status;

    @Column(
            name = "total_files",
            nullable = false
    )
    private Integer totalFiles = 0;

    @Column(
            name = "processed_files",
            nullable = false
    )
    private Integer processedFiles = 0;

    @Column(
            name = "total_chunks",
            nullable = false
    )
    private Integer totalChunks = 0;

    @Column(
            name = "processed_chunks",
            nullable = false
    )
    private Integer processedChunks = 0;

    @Column(
            name = "error_message",
            columnDefinition = "TEXT"
    )
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Integer getProcessedFiles() {
        return processedFiles;
    }

    public void setProcessedFiles(Integer processedFiles) {
        this.processedFiles = processedFiles;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getProcessedChunks() {
        return processedChunks;
    }

    public void setProcessedChunks(Integer processedChunks) {
        this.processedChunks = processedChunks;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}