package com.codecommitai.repositoryfile.repository;

import com.codecommitai.repositoryfile.entity.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryFileRepository
        extends JpaRepository<RepositoryFile, UUID> {

    Optional<RepositoryFile> findByRepositoryIdAndPath(
            UUID repositoryId,
            String path
    );

    boolean existsByRepositoryIdAndPath(
            UUID repositoryId,
            String path
    );

    List<RepositoryFile> findAllByRepositoryId(
            UUID repositoryId
    );

    void deleteAllByRepositoryId(UUID repositoryId);
}