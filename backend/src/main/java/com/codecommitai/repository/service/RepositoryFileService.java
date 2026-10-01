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

    private static final Set<String> EXCLUDED_FILES = Set.of(
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

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Repository not found: " + repositoryId
                        )
                );

        String branch = repository.getDefaultBranch();

        if (branch == null || branch.isBlank()) {
            branch = "main";
        }

        GitHubTreeResponse tree = gitHubService.getRepositoryTree(
                repository.getOwner(),
                repository.getName(),
                branch
        );

        if (tree == null) {
            throw new IllegalStateException(
                    "GitHub repository tree response is null"
            );
        }

        if (tree.truncated()) {
            throw new IllegalStateException(
                    "GitHub repository tree is truncated; indexing cannot continue safely"
            );
        }

        List<RepositoryFile> newFiles = new ArrayList<>();
        List<RepositoryFile> modifiedFiles = new ArrayList<>();
        List<RepositoryFile> unchangedFiles = new ArrayList<>();

        Set<String> currentPaths = new HashSet<>();

        if (tree.tree() != null) {

            for (GitHubTreeResponse.TreeEntry entry : tree.tree()) {

                if (entry == null) {
                    continue;
                }

                if (!"blob".equalsIgnoreCase(entry.type())) {
                    continue;
                }

                String path = entry.path();

                if (!isSupportedFile(path)) {
                    continue;
                }

                currentPaths.add(path);

                RepositoryFile existing =
                        repositoryFileRepository
                                .findByRepositoryIdAndPath(repositoryId, path)
                                .orElse(null);

                /*
                 * NEW FILE
                 */
                if (existing == null) {

                    RepositoryFile repositoryFile =
                            createRepositoryFile(repository, entry);

                    /*
                     * The SHA is intentionally NOT written to githubSha.
                     *
                     * It stays transient until the complete indexing
                     * pipeline succeeds.
                     */
                    repositoryFile.setPendingGithubSha(entry.sha());

                    repositoryFileRepository.save(repositoryFile);

                    newFiles.add(repositoryFile);

                    continue;
                }

                /*
                 * EXISTING FILE
                 */
                String previousSha = existing.getGithubSha();
                String currentSha = entry.sha();

                updateMetadata(existing, entry);

                /*
                 * Store the newly discovered SHA only in memory.
                 *
                 * githubSha remains the SHA of the last successfully
                 * indexed version.
                 */
                existing.setPendingGithubSha(currentSha);

                /*
                 * UNCHANGED FILE
                 */
                if (previousSha != null && previousSha.equals(currentSha)) {

                    existing.setPendingGithubSha(null);

                    repositoryFileRepository.save(existing);

                    unchangedFiles.add(existing);

                    continue;
                }

                /*
                 * MODIFIED FILE
                 */
                repositoryFileRepository.save(existing);

                modifiedFiles.add(existing);
            }
        }

        /*
         * Anything previously indexed but missing from the current
         * GitHub tree has been deleted from the repository.
         */
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

    /**
     * Commits newly discovered GitHub SHAs as successfully indexed.
     *
     * This method must only be called after:
     *
     * 1. Content download succeeds
     * 2. Chunking succeeds
     * 3. Embedding generation succeeds
     *
     * If any previous stage fails, this method is never called and
     * githubSha continues to represent the previous successfully
     * indexed version.
     */
    @Transactional
    public void markFilesAsIndexed(List<RepositoryFile> files) {

        if (files == null || files.isEmpty()) {
            return;
        }

        for (RepositoryFile file : files) {

            String pendingSha = file.getPendingGithubSha();

            if (pendingSha == null || pendingSha.isBlank()) {
                continue;
            }

            file.setGithubSha(pendingSha);
            file.setPendingGithubSha(null);

            repositoryFileRepository.save(file);
        }

        repositoryFileRepository.flush();
    }

    /**
     * Compatibility method for existing callers.
     */
    @Transactional
    public List<RepositoryFile> discoverFiles(UUID repositoryId) {

        RepositoryFileChangeSet changeSet =
                discoverFilesWithChanges(repositoryId);

        List<RepositoryFile> files = new ArrayList<>();

        files.addAll(changeSet.newFiles());
        files.addAll(changeSet.modifiedFiles());
        files.addAll(changeSet.unchangedFiles());

        return files;
    }

    private RepositoryFile createRepositoryFile(
            Repository repository,
            GitHubTreeResponse.TreeEntry entry
    ) {

        RepositoryFile repositoryFile = new RepositoryFile();

        repositoryFile.setRepository(repository);
        repositoryFile.setPath(entry.path());
        repositoryFile.setFileName(extractFileName(entry.path()));
        repositoryFile.setExtension(extractExtension(entry.path()));
        repositoryFile.setLanguage(detectLanguage(entry.path()));

        /*
         * Do not set githubSha here.
         *
         * It will be committed only after successful indexing.
         */
        repositoryFile.setGithubSha(null);

        repositoryFile.setFileSizeBytes(entry.size());

        return repositoryFile;
    }

    private void updateMetadata(
            RepositoryFile repositoryFile,
            GitHubTreeResponse.TreeEntry entry
    ) {

        repositoryFile.setFileName(
                extractFileName(entry.path())
        );

        repositoryFile.setExtension(
                extractExtension(entry.path())
        );

        repositoryFile.setLanguage(
                detectLanguage(entry.path())
        );

        repositoryFile.setFileSizeBytes(entry.size());
    }

    private boolean isSupportedFile(String path) {

        if (path == null || path.isBlank()) {
            return false;
        }

        String normalizedPath =
                path.replace('\\', '/')
                        .toLowerCase(Locale.ROOT);

        for (String directory : EXCLUDED_DIRECTORIES) {

            if (normalizedPath.contains("/" + directory)
                    || normalizedPath.startsWith(directory)) {
                return false;
            }
        }

        String fileName = extractFileName(normalizedPath);

        if (EXCLUDED_FILES.contains(fileName)) {
            return false;
        }

        String extension = extractExtension(normalizedPath);

        if (extension == null || extension.isBlank()) {
            return EXTENSIONLESS_FILES.contains(fileName);
        }

        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    private String extractFileName(String path) {

        int slashIndex = path.lastIndexOf('/');

        if (slashIndex < 0) {
            return path;
        }

        return path.substring(slashIndex + 1);
    }

    private String extractExtension(String path) {

        String fileName = extractFileName(path);

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String detectLanguage(String path) {

        String fileName =
                extractFileName(path)
                        .toLowerCase(Locale.ROOT);

        String extension = extractExtension(path);

        return switch (extension) {

            case "java" -> "Java";

            case "js" -> "JavaScript";

            case "jsx" -> "JavaScript React";

            case "ts" -> "TypeScript";

            case "tsx" -> "TypeScript React";

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

            case "h" -> "C Header";

            case "cpp", "cc", "cxx" -> "C++";

            case "hpp" -> "C++ Header";

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

            default -> switch (fileName) {

                case "dockerfile" -> "Dockerfile";

                case "makefile" -> "Makefile";

                case "jenkinsfile" -> "Jenkinsfile";

                case "license" -> "License";

                case "readme" -> "Markdown";

                default -> "Unknown";
            };
        };
    }
}