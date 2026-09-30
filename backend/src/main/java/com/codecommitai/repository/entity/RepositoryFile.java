package com.codecommitai.repositoryfile.entity;

import com.codecommitai.repository.entity.Repository;
import jakarta.persistence.*;

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
    @JoinColumn(
            name = "repository_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_repository_files_repository"
            )
    )
    private Repository repository;

    @Column(
            name = "path",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String path;

    @Column(
            name = "file_name",
            nullable = false,
            length = 255
    )
    private String fileName;

    @Column(length = 50)
    private String extension;

    @Column(length = 100)
    private String language;

    @Column(name = "github_sha", length = 255)
    private String githubSha;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(columnDefinition = "TEXT")
    private String content;

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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}