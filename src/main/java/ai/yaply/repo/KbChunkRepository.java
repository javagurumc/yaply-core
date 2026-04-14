package ai.yaply.repo;

import ai.yaply.entity.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for KbChunk entities with vector similarity search.
 */
public interface KbChunkRepository extends JpaRepository<KbChunk, UUID> {

    /**
     * Vector similarity search using pgvector cosine distance.
     * Returns chunks ordered by similarity (highest first).
     *
     * @param queryEmbedding Vector embedding in pgvector format: "[0.1, 0.2, ...]"
     * @param scope          Knowledge scope to filter by
     * @param userId         User ID for USER scope (null for others)
     * @param source         Optional source filter (null to skip)
     * @param topK           Number of results to return
     * @return Array of Object[] with: [chunkId, documentId, chunkIndex, chunkText,
     *         embedding, createdAt, scope, userId, source, title, tags, score]
     */
    @Query(value = """
            SELECT
                c.id, c.document_id, c.chunk_index, c.chunk_text, c.embedding, c.created_at,
                d.scope, d.user_id, d.source, d.title, d.tags,
                1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) AS score
            FROM kb_chunk c
            JOIN kb_document d ON d.id = c.document_id
            WHERE d.scope = CAST(:scope AS TEXT)
              AND (:userId IS NULL OR d.user_id = :userId)
              AND (:source IS NULL OR d.source = :source)
            ORDER BY c.embedding <=> CAST(:queryEmbedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> searchSimilar(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("scope") String scope,
            @Param("userId") String userId,
            @Param("source") String source,
            @Param("topK") int topK);

    /**
     * Find all chunks for a document.
     */
    List<KbChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    /**
     * Delete all chunks for a document (for re-indexing).
     */
    @Modifying
    @Query("DELETE FROM KbChunk c WHERE c.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Count chunks for a document.
     */
    long countByDocumentId(UUID documentId);
}
