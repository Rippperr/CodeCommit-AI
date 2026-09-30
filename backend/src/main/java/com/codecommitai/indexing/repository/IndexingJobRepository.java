package com.codecommitai.indexing.repository;

import com.codecommitai.indexing.entity.IndexingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IndexingJobRepository
        extends JpaRepository<IndexingJob, UUID> {

    List<IndexingJob> findAllByRepositoryIdOrderByCreatedAtDesc(
            UUID repositoryId
    );

    List<IndexingJob> findAllByStatus(
            String status
    );
}