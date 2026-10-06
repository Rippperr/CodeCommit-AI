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
                    LEAST(
                        1.00,
                        0.50
                        +
                        (
                            SELECT COUNT(*)
                            FROM regexp_split_to_table(
                                LOWER(:query),
                                '\\s+'
                            ) AS query_token
                            WHERE LENGTH(query_token) > 0
                              AND (
                                    LOWER(rf.path)
                                        LIKE '%' || query_token || '%'

                                    OR LOWER(rf.file_name)
                                        LIKE '%' || query_token || '%'

                                    OR LOWER(c.content)
                                        LIKE '%' || query_token || '%'
                              )
                        ) * 0.15
                    )
                    AS double precision
                ) AS relevanceScore

            FROM code_chunks c

            INNER JOIN repository_files rf
                ON rf.id = c.repository_file_id

            WHERE rf.repository_id = :repositoryId

              AND EXISTS (
                    SELECT 1
                    FROM regexp_split_to_table(
                        LOWER(:query),
                        '\\s+'
                    ) AS query_token
                    WHERE LENGTH(query_token) > 0
                      AND (
                            LOWER(rf.path)
                                LIKE '%' || query_token || '%'

                            OR LOWER(rf.file_name)
                                LIKE '%' || query_token || '%'

                            OR LOWER(c.content)
                                LIKE '%' || query_token || '%'
                      )
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
            @Param("limit") int limit
    );
}