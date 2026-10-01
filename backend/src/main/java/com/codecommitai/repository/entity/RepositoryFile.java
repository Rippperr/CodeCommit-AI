package com.codecommitai.repositoryfile.entity;

import com.codecommitai.repository.entity.Repository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "repository_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_repository_file_path",
                        columnNames = {"repository_id", "path"}
                )
        }
)
public class RepositoryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(nullable = false)
    private String path;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    private String extension;

    private String language;

    /**
     * SHA of the last GitHub version that was successfully indexed.
     *
     * This value is NOT changed during repository discovery.
     * It is updated only after content download, chunking and
     * embedding generation complete successfully.
     */
    @Column(name = "github_sha")
    private String githubSha;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(columnDefinition = "text")
    private String content;

    /**
     * Newly discovered GitHub SHA waiting for successful indexing.
     *
     * This field is transient and is NOT stored in PostgreSQL.
     */
    @Transient
    private String pendingGithubSha;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGithubSha() {
        return githubSha;
    }

    public void setGithubSha(String githubSha) {
        this.githubSha = githubSha;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPendingGithubSha() {
        return pendingGithubSha;
    }

    public void setPendingGithubSha(String pendingGithubSha) {
        this.pendingGithubSha = pendingGithubSha;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}