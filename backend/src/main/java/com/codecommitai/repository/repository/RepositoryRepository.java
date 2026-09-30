package com.codecommitai.repository.repository;

import com.codecommitai.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    Optional<Repository> findByGithubId(Long githubId);

    Optional<Repository> findByFullName(String fullName);

    boolean existsByGithubId(Long githubId);

    boolean existsByFullName(String fullName);
}