package com.codecommitai.repository.service;

import com.codecommitai.exception.DuplicateResourceException;
import com.codecommitai.exception.ResourceNotFoundException;
import com.codecommitai.repository.dto.CreateRepositoryRequest;
import com.codecommitai.repository.dto.RepositoryResponse;
import com.codecommitai.repository.entity.Repository;
import com.codecommitai.repository.repository.RepositoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;

    public RepositoryService(RepositoryRepository repositoryRepository) {
        this.repositoryRepository = repositoryRepository;
    }

    public RepositoryResponse create(CreateRepositoryRequest request) {

        if (repositoryRepository.existsByGithubId(request.githubId())) {
            throw new DuplicateResourceException(
                    "Repository with GitHub ID already exists: " + request.githubId()
            );
        }

        if (repositoryRepository.existsByFullName(request.fullName())) {
            throw new DuplicateResourceException(
                    "Repository already exists: " + request.fullName()
            );
        }

        Repository repository = new Repository();

        repository.setGithubId(request.githubId());
        repository.setOwner(request.owner());
        repository.setName(request.name());
        repository.setFullName(request.fullName());
        repository.setDefaultBranch(request.defaultBranch());
        repository.setDescription(request.description());
        repository.setHtmlUrl(request.htmlUrl());
        repository.setPrivateRepository(request.privateRepository());

        Repository savedRepository = repositoryRepository.save(repository);

        return toResponse(savedRepository);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> findAll() {

        return repositoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepositoryResponse findById(UUID id) {

        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repository not found: " + id
                        )
                );

        return toResponse(repository);
    }

    private RepositoryResponse toResponse(Repository repository) {

        return new RepositoryResponse(
                repository.getId(),
                repository.getGithubId(),
                repository.getOwner(),
                repository.getName(),
                repository.getFullName(),
                repository.getDefaultBranch(),
                repository.getDescription(),
                repository.getHtmlUrl(),
                repository.isPrivateRepository(),
                repository.getCreatedAt(),
                repository.getUpdatedAt()
        );
    }
}