-- ============================================================
-- CodeCommit AI - Initial Database Schema
-- Version: V1
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- REPOSITORIES
-- ============================================================

CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    github_id BIGINT NOT NULL UNIQUE,
    owner VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(511) NOT NULL UNIQUE,

    default_branch VARCHAR(255),

    description TEXT,
    html_url TEXT,

    is_private BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_repositories_owner
    ON repositories(owner);

-- ============================================================
-- REPOSITORY FILES
-- ============================================================

CREATE TABLE repository_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    repository_id UUID NOT NULL,

    path TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    extension VARCHAR(50),

    language VARCHAR(100),

    github_sha VARCHAR(255),

    file_size_bytes BIGINT,

    content TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_repository_files_repository
        FOREIGN KEY (repository_id)
        REFERENCES repositories(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_repository_file_path
        UNIQUE (repository_id, path)
);

CREATE INDEX idx_repository_files_repository_id
    ON repository_files(repository_id);

CREATE INDEX idx_repository_files_language
    ON repository_files(language);

-- ============================================================
-- CODE CHUNKS
-- ============================================================

CREATE TABLE code_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    repository_file_id UUID NOT NULL,

    chunk_index INTEGER NOT NULL,

    content TEXT NOT NULL,

    start_line INTEGER,
    end_line INTEGER,

    token_count INTEGER,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_code_chunks_repository_file
        FOREIGN KEY (repository_file_id)
        REFERENCES repository_files(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_code_chunk_index
        UNIQUE (repository_file_id, chunk_index)
);

CREATE INDEX idx_code_chunks_repository_file_id
    ON code_chunks(repository_file_id);

-- ============================================================
-- EMBEDDINGS
-- ============================================================

CREATE TABLE embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code_chunk_id UUID NOT NULL UNIQUE,

    embedding vector(1536),

    model VARCHAR(255) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_embeddings_code_chunk
        FOREIGN KEY (code_chunk_id)
        REFERENCES code_chunks(id)
        ON DELETE CASCADE
);

-- Vector similarity search index
CREATE INDEX idx_embeddings_vector
    ON embeddings
    USING hnsw (embedding vector_cosine_ops);

-- ============================================================
-- INDEXING JOBS
-- ============================================================

CREATE TABLE indexing_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    repository_id UUID NOT NULL,

    status VARCHAR(50) NOT NULL,

    total_files INTEGER NOT NULL DEFAULT 0,
    processed_files INTEGER NOT NULL DEFAULT 0,

    total_chunks INTEGER NOT NULL DEFAULT 0,
    processed_chunks INTEGER NOT NULL DEFAULT 0,

    error_message TEXT,

    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_indexing_jobs_repository
        FOREIGN KEY (repository_id)
        REFERENCES repositories(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_indexing_jobs_repository_id
    ON indexing_jobs(repository_id);

CREATE INDEX idx_indexing_jobs_status
    ON indexing_jobs(status);