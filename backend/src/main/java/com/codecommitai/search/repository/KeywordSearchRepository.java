package com.codecommitai.search.repository;

import com.codecommitai.codechunk.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KeywordSearchRepository
        extends JpaRepository<CodeChunk, UUID> {

    @Query(value = """
            SELECT
                rf.path AS filePath,
                rf.file_name AS fileName,
                c.chunk_index AS chunkIndex,
                c.start_line AS startLine,
                c.end_line AS endLine,
                c.content AS content,
                CAST(
                    CASE
                        WHEN LOWER(rf.path) = LOWER(:query)
                            THEN 1.00
                        WHEN LOWER(rf.file_name) = LOWER(:query)
                            THEN 0.95
                        WHEN LOWER(rf.path) LIKE LOWER(:query || '%')
                            THEN 0.90
                        WHEN LOWER(rf.file_name) LIKE LOWER(:query || '%')
                            THEN 0.85
                        ELSE 0.70
                    END
                    AS double precision
                ) AS relevanceScore
            FROM code_chunks c
            INNER JOIN repository_files rf
                ON rf.id = c.repository_file_id
            WHERE rf.repository_id = :repositoryId
              AND (
                    rf.path ILIKE '%' || :escapedQuery || '%' ESCAPE '\\'
                    OR rf.file_name ILIKE '%' || :escapedQuery || '%' ESCAPE '\\'
                    OR c.content ILIKE '%' || :escapedQuery || '%' ESCAPE '\\'
              )
            ORDER BY
                relevanceScore DESC,
                rf.path ASC,
                c.chunk_index ASC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<KeywordSearchProjection> search(
            @Param("repositoryId") UUID repositoryId,
            @Param("query") String query,
            @Param("escapedQuery") String escapedQuery,
            @Param("limit") int limit
    );
}