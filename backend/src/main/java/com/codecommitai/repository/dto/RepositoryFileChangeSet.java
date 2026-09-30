package com.codecommitai.repository.dto;

import com.codecommitai.repositoryfile.entity.RepositoryFile;

import java.util.List;

public record RepositoryFileChangeSet(
        List<RepositoryFile> newFiles,
        List<RepositoryFile> modifiedFiles,
        List<RepositoryFile> unchangedFiles,
        List<RepositoryFile> deletedFiles
) {

    public int totalChangedFiles() {
        return newFiles.size() + modifiedFiles.size();
    }

    public int totalFiles() {
        return newFiles.size()
                + modifiedFiles.size()
                + unchangedFiles.size();
    }
}