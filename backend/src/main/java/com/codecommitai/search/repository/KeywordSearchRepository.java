
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
            WITH query_tokens AS (
                SELECT DISTINCT token
                FROM regexp_split_to_table(
                    LOWER(:query),
                    '[^a-z0-9_]+'
                ) AS token
                WHERE LENGTH(token) > 0
                  AND token NOT IN (
                      'a', 'an', 'and', 'are', 'as', 'at',
                      'be', 'been', 'being', 'but', 'by',
                      'can', 'could', 'describe', 'do', 'does',
                      'for', 'from', 'how', 'in', 'into', 'is',
                      'it', 'its', 'of', 'on', 'or', 'that',
                      'the', 'their', 'them', 'then', 'there',
                      'these', 'this', 'those', 'to', 'was',
                      'were', 'what', 'when', 'where', 'which',
                      'who', 'why', 'will', 'with', 'would',
                      'work', 'works', 'working', 'exactly',
                      'actually', 'please', 'explain', 'repository'
                  )
            ),
            scored_chunks AS (
                SELECT
                    rf.path AS filePath,
                    rf.file_name AS fileName,
                    c.chunk_index AS chunkIndex,
                    c.start_line AS startLine,
                    c.end_line AS endLine,
                    c.content AS content,

                    COALESCE(
                        SUM(
                            CASE
                                WHEN LOWER(rf.file_name)
                                     LIKE '%' || qt.token || '%'
                                THEN 3.0

                                WHEN LOWER(rf.path)
                                     LIKE '%' || qt.token || '%'
                                THEN 2.0

                                WHEN LOWER(c.content)
                                     LIKE '%' || qt.token || '%'
                                THEN 1.0

                                ELSE 0.0
                            END
                        ),
                        0.0
                    ) AS matched_weight,

                    (SELECT COUNT(*) FROM query_tokens)
                        AS total_tokens

                FROM code_chunks c
                INNER JOIN repository_files rf
                    ON rf.id = c.repository_file_id

                CROSS JOIN query_tokens qt

                WHERE rf.repository_id = :repositoryId

                GROUP BY
                    rf.path,
                    rf.file_name,
                    c.chunk_index,
                    c.start_line,
                    c.end_line,
                    c.content
            )
            SELECT
                filePath,
                fileName,
                chunkIndex,
                startLine,
                endLine,
                content,

                CAST(
                    CASE
                        WHEN total_tokens = 0 THEN 0.0
                        ELSE matched_weight
                             / (total_tokens * 3.0)
                    END
                    AS double precision
                ) AS relevanceScore

            FROM scored_chunks
            WHERE matched_weight > 0

            ORDER BY
                relevanceScore DESC,
                matched_weight DESC,
                filePath ASC,
                chunkIndex ASC

            LIMIT :limit
            """,
            nativeQuery = true)
    List<KeywordSearchProjection> search(
            @Param("repositoryId") UUID repositoryId,
            @Param("query") String query,
            @Param("limit") int limit
    );
}
