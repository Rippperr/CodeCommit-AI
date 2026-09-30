package com.codecommitai.repository.controller;

import com.codecommitai.repository.dto.CreateRepositoryRequest;
import com.codecommitai.repository.dto.RepositoryResponse;
import com.codecommitai.repository.service.RepositoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse create(
            @Valid @RequestBody CreateRepositoryRequest request
    ) {
        return repositoryService.create(request);
    }

    @GetMapping
    public List<RepositoryResponse> findAll() {
        return repositoryService.findAll();
    }

    @GetMapping("/{id}")
    public RepositoryResponse findById(@PathVariable UUID id) {
        return repositoryService.findById(id);
    }
}