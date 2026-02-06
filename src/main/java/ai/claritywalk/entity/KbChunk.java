package ai.claritywalk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Knowledge Base Chunk entity.
 * Stores text chunks with vector embeddings for similarity search.
 * Embedding stored as String for native query compatibility with pgvector.
 */
@Entity
@Table(name = "kb_chunk")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KbChunk {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    /**
     * Vector embedding stored as String in pgvector format: "[0.1, 0.2, ...]"
     * Dimension: 3072 (OpenAI text-embedding-3-large)
     */
    @Column(nullable = false, columnDefinition = "vector(3072)")
    @org.hibernate.annotations.ColumnTransformer(write = "?::vector")
    private String embedding;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * Factory method for creating a new chunk.
     */
    public static KbChunk create(UUID documentId, int chunkIndex, String chunkText, String embedding) {
        KbChunk chunk = new KbChunk();
        chunk.documentId = documentId;
        chunk.chunkIndex = chunkIndex;
        chunk.chunkText = chunkText;
        chunk.embedding = embedding;
        chunk.createdAt = Instant.now();
        return chunk;
    }
}
