package com.codecommitai.embedding.entity;

import com.codecommitai.codechunk.entity.CodeChunk;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "embeddings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "embeddings_code_chunk_id_key",
                        columnNames = "code_chunk_id"
                )
        }
)
public class Embedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "code_chunk_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_embeddings_code_chunk"
            )
    )
    private CodeChunk codeChunk;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding")
    private float[] embedding;

    @Column(
            name = "model",
            nullable = false,
            length = 255
    )
    private String model;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public CodeChunk getCodeChunk() {
        return codeChunk;
    }

    public void setCodeChunk(CodeChunk codeChunk) {
        this.codeChunk = codeChunk;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}