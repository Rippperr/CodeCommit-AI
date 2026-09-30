package com.codecommitai.repositoryfile.service;

import com.codecommitai.github.dto.GitHubTreeResponse;
import com.codecommitai.github.service.GitHubService;
import com.codecommitai.repository.dto.RepositoryFileChangeSet;
import com.codecommitai.repository.entity.Repository;
import com.codecommitai.repository.repository.RepositoryRepository;
import com.codecommitai.repositoryfile.entity.RepositoryFile;
import com.codecommitai.repositoryfile.repository.RepositoryFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RepositoryFileService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final GitHubService gitHubService;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java",
            "js",
            "jsx",
            "ts",
            "tsx",
            "html",
            "htm",
            "css",
            "scss",
            "sass",
            "less",
            "json",
            "xml",
            "yaml",
            "yml",
            "properties",
            "sql",
            "md",
            "txt",
            "py",
            "c",
            "h",
            "cpp",
            "cc",
            "cxx",
            "hpp",
            "cs",
            "go",
            "rs",
            "php",
            "rb",
            "swift",
            "kt",
            "kts",
            "dart",
            "sh",
            "bash",
            "zsh",
            "ps1",
            "bat",
            "cmd",
            "vue",
            "svelte"
    );

    private static final Set<String> EXTENSIONLESS_FILES = Set.of(
            "license",
            "dockerfile",
            "makefile",
            "jenkinsfile",
            "vagrantfile",
            "procfile",
            ".gitignore",
            ".gitattributes",
            ".dockerignore",
            ".editorconfig",
            ".npmrc",
            ".nvmrc",
            ".prettierrc",
            ".eslintrc",
            "readme"
    );

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git/",
            "node_modules/",
            "target/",
            "build/",
            "dist/",
            "out/",
            ".idea/",
            ".vscode/",
            "__pycache__/",
            ".gradle/",
            "coverage/",
            ".next/",
            ".nuxt/",
            "vendor/"
    );

    private static final Set<String> EXCLUDED_FILE_NAMES = Set.of(
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "composer.lock",
            "gradle.lockfile"
    );

    public RepositoryFileService(
            RepositoryRepository repositoryRepository,
            RepositoryFileRepository repositoryFileRepository,
            GitHubService gitHubService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.gitHubService = gitHubService;
    }

    @Transactional
    public RepositoryFileChangeSet discoverFilesWithChanges(UUID repositoryId) {

        Repository repository = repositoryRepository
                .findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Repository not found: " + repositoryId
                ));

        String owner = repository.getOwner();
        String repositoryName = repository.getName();

        String branch = repository.getDefaultBranch();

        if (branch == null || branch.isBlank()) {
            branch = "main";
        }

        GitHubTreeResponse treeResponse =
                gitHubService.getRepositoryTree(
                        owner,
                        repositoryName,
                        branch
                );

        if (treeResponse == null) {
            throw new IllegalStateException(
                    "GitHub returned an empty tree response"
            );
        }

        if (treeResponse.truncated()) {
            throw new IllegalStateException(
                    "GitHub repository tree is truncated. " +
                    "The repository is too large for a single recursive tree request."
            );
        }

        if (treeResponse.tree() == null) {
            return new RepositoryFileChangeSet(
                    List.of(),
                    List.of(),
                    List.of(),
                    repositoryFileRepository.findAllByRepositoryId(repositoryId)
            );
        }

        List<RepositoryFile> newFiles = new ArrayList<>();
        List<RepositoryFile> modifiedFiles = new ArrayList<>();
        List<RepositoryFile> unchangedFiles = new ArrayList<>();

        Set<String> currentPaths = new HashSet<>();

        for (GitHubTreeResponse.TreeEntry entry : treeResponse.tree()) {

            if (!"blob".equalsIgnoreCase(entry.type())) {
                continue;
            }

            if (!isSupportedFile(entry.path())) {
                continue;
            }

            currentPaths.add(entry.path());

            RepositoryFile repositoryFile =
                    repositoryFileRepository
                            .findByRepositoryIdAndPath(
                                    repositoryId,
                                    entry.path()
                            )
                            .orElse(null);

            if (repositoryFile == null) {
                repositoryFile = new RepositoryFile();

                repositoryFile.setRepository(repository);
                repositoryFile.setPath(entry.path());

                repositoryFile.setFileName(
                        extractFileName(entry.path())
                );

                repositoryFile.setExtension(
                        extractExtension(entry.path())
                );

                repositoryFile.setLanguage(
                        detectLanguage(entry.path())
                );

                repositoryFile.setGithubSha(entry.sha());

                repositoryFile.setFileSizeBytes(entry.size());

                /*
                 * Content is intentionally not downloaded yet.
                 * The indexing pipeline will download it after discovery.
                 */
                RepositoryFile savedFile =
                        repositoryFileRepository.save(repositoryFile);

                newFiles.add(savedFile);
                continue;
            }

            String previousSha = repositoryFile.getGithubSha();

            repositoryFile.setRepository(repository);
            repositoryFile.setPath(entry.path());

            repositoryFile.setFileName(
                    extractFileName(entry.path())
            );

            repositoryFile.setExtension(
                    extractExtension(entry.path())
            );

            repositoryFile.setLanguage(
                    detectLanguage(entry.path())
            );

            repositoryFile.setGithubSha(entry.sha());
            repositoryFile.setFileSizeBytes(entry.size());

            RepositoryFile savedFile =
                    repositoryFileRepository.save(repositoryFile);

            if (previousSha == null || previousSha.isBlank()) {
                modifiedFiles.add(savedFile);
            } else if (!previousSha.equals(entry.sha())) {
                modifiedFiles.add(savedFile);
            } else {
                unchangedFiles.add(savedFile);
            }
        }

        List<RepositoryFile> existingFiles =
                repositoryFileRepository.findAllByRepositoryId(repositoryId);

        List<RepositoryFile> deletedFiles = existingFiles.stream()
                .filter(file -> !currentPaths.contains(file.getPath()))
                .toList();

        return new RepositoryFileChangeSet(
                newFiles,
                modifiedFiles,
                unchangedFiles,
                deletedFiles
        );
    }

    /*
     * Kept for compatibility with the existing repository-file discovery
     * endpoint and any code that still expects the original method.
     */
    @Transactional
    public List<RepositoryFile> discoverFiles(UUID repositoryId) {
        RepositoryFileChangeSet changeSet =
                discoverFilesWithChanges(repositoryId);

        List<RepositoryFile> discoveredFiles = new ArrayList<>();

        discoveredFiles.addAll(changeSet.newFiles());
        discoveredFiles.addAll(changeSet.modifiedFiles());
        discoveredFiles.addAll(changeSet.unchangedFiles());

        return discoveredFiles;
    }

    private boolean isSupportedFile(String path) {

        if (path == null || path.isBlank()) {
            return false;
        }

        String normalizedPath =
                path.toLowerCase(Locale.ROOT);

        for (String excludedDirectory : EXCLUDED_DIRECTORIES) {
            if (normalizedPath.contains(excludedDirectory)) {
                return false;
            }
        }

        String fileName =
                extractFileName(normalizedPath);

        if (EXCLUDED_FILE_NAMES.contains(fileName)) {
            return false;
        }

        if (EXTENSIONLESS_FILES.contains(fileName)) {
            return true;
        }

        String extension =
                extractExtension(normalizedPath);

        return extension != null
                && SUPPORTED_EXTENSIONS.contains(extension);
    }

    private String extractFileName(String path) {

        if (path == null || path.isBlank()) {
            return "";
        }

        int separatorIndex =
                path.lastIndexOf('/');

        if (separatorIndex < 0) {
            return path;
        }

        return path.substring(separatorIndex + 1);
    }

    private String extractExtension(String path) {

        String fileName =
                extractFileName(path);

        int dotIndex =
                fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String detectLanguage(String path) {

        String fileName =
                extractFileName(path)
                        .toLowerCase(Locale.ROOT);

        if (fileName.equals("dockerfile")) {
            return "Dockerfile";
        }

        if (fileName.equals("makefile")) {
            return "Makefile";
        }

        if (fileName.equals("jenkinsfile")) {
            return "Jenkinsfile";
        }

        if (fileName.equals("license")) {
            return "Text";
        }

        if (fileName.equals("readme")) {
            return "Markdown";
        }

        if (fileName.equals(".gitignore")
                || fileName.equals(".gitattributes")
                || fileName.equals(".dockerignore")
                || fileName.equals(".editorconfig")
                || fileName.equals(".npmrc")
                || fileName.equals(".nvmrc")
                || fileName.equals(".prettierrc")
                || fileName.equals(".eslintrc")) {
            return "Configuration";
        }

        String extension =
                extractExtension(path);

        if (extension == null) {
            return "Unknown";
        }

        return switch (extension) {
            case "java" -> "Java";
            case "js" -> "JavaScript";
            case "jsx" -> "JavaScript";
            case "ts" -> "TypeScript";
            case "tsx" -> "TypeScript";
            case "html", "htm" -> "HTML";
            case "css" -> "CSS";
            case "scss" -> "SCSS";
            case "sass" -> "Sass";
            case "less" -> "Less";
            case "json" -> "JSON";
            case "xml" -> "XML";
            case "yaml", "yml" -> "YAML";
            case "properties" -> "Properties";
            case "sql" -> "SQL";
            case "md" -> "Markdown";
            case "txt" -> "Text";
            case "py" -> "Python";
            case "c" -> "C";
            case "h" -> "C";
            case "cpp", "cc", "cxx" -> "C++";
            case "hpp" -> "C++";
            case "cs" -> "C#";
            case "go" -> "Go";
            case "rs" -> "Rust";
            case "php" -> "PHP";
            case "rb" -> "Ruby";
            case "swift" -> "Swift";
            case "kt", "kts" -> "Kotlin";
            case "dart" -> "Dart";
            case "sh", "bash", "zsh" -> "Shell";
            case "ps1" -> "PowerShell";
            case "bat", "cmd" -> "Batch";
            case "vue" -> "Vue";
            case "svelte" -> "Svelte";
            default -> "Unknown";
        };
    }
}