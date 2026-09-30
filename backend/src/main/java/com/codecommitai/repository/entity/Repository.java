package com.codecommitai.repository.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "repositories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "repositories_github_id_key",
            columnNames = "github_id"
        ),
        @UniqueConstraint(
            name = "repositories_full_name_key",
            columnNames = "full_name"
        )
    }
)
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(nullable = false, length = 255)
    private String owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", nullable = false, length = 511, unique = true)
    private String fullName;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "html_url", columnDefinition = "TEXT")
    private String htmlUrl;

    @Column(name = "is_private", nullable = false)
    private boolean privateRepository;

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

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public Long getGithubId() {
        return githubId;
    }

    public void setGithubId(Long githubId) {
        this.githubId = githubId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public boolean isPrivateRepository() {
        return privateRepository;
    }

    public void setPrivateRepository(boolean privateRepository) {
        this.privateRepository = privateRepository;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}