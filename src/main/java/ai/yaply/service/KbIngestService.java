package ai.yaply.service;

import ai.yaply.entity.KbChunk;
import ai.yaply.entity.KbDocument;
import ai.yaply.entity.KbScope;
import ai.yaply.repo.KbChunkRepository;
import ai.yaply.repo.KbDocumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for ingesting documents into the knowledge base.
 * Handles chunking, embedding, and storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbIngestService {

    private final KbDocumentRepository documentRepo;
    private final KbChunkRepository chunkRepo;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    /**
     * Ingest a document into the knowledge base.
     * Creates or updates document, chunks content, generates embeddings, and stores
     * chunks.
     *
     * @param scope   Knowledge scope
     * @param userId  User ID (required for USER scope)
     * @param source  Document source
     * @param title   Document title
     * @param content Document content
     * @param tags    Tags for categorization
     * @param version Version number
     * @return Document ID and number of chunks created
     */
    @Transactional
    public UUID ingestDocument(
            KbScope scope,
            String userId,
            String source,
            String title,
            String content,
            List<String> tags,
            int version) {
        log.info("Ingesting document: scope={}, userId={}, source={}, title={}",
                scope, userId, source, title);

        // Validate scope/userId combination
        if (scope == KbScope.USER && (userId == null || userId.isBlank())) {
            throw new IllegalArgumentException("userId is required for USER scope");
        }
        if (scope != KbScope.USER && userId != null) {
            throw new IllegalArgumentException("userId must be null for non-USER scopes");
        }

        // 1. Upsert document
        KbDocument document = upsertDocument(scope, userId, source, title, content, tags, version);
        log.info("Document upserted with ID: {}", document.getId());

        // 2. Delete old chunks (if re-indexing)
        chunkRepo.deleteByDocumentId(document.getId());
        log.info("Deleted old chunks for document {}", document.getId());

        // 3. Chunk content
        List<String> chunks = chunkingService.chunkText(content);
        log.info("Created {} chunks from content", chunks.size());

        if (chunks.isEmpty()) {
            log.warn("No chunks created for document {}", document.getId());
            return document.getId();
        }

        // 4. Generate embeddings for all chunks
        List<float[]> embeddings = embeddingService.embedBatch(chunks);

        if (embeddings.size() != chunks.size()) {
            throw new RuntimeException("Embedding count mismatch: expected " + chunks.size() +
                    ", got " + embeddings.size());
        }

        // 5. Save chunks with embeddings
        for (int i = 0; i < chunks.size(); i++) {
            String embeddingStr = embeddingService.formatVector(embeddings.get(i));
            KbChunk chunk = KbChunk.create(document.getId(), i, chunks.get(i), embeddingStr);
            chunkRepo.save(chunk);
        }

        log.info("Successfully ingested document {} with {} chunks", document.getId(), chunks.size());
        return document.getId();
    }

    /**
     * Upsert document (create or update).
     * Checks if document with same title + scope + userId exists.
     */
    private KbDocument upsertDocument(
            KbScope scope,
            String userId,
            String source,
            String title,
            String content,
            List<String> tags,
            int version) {
        Optional<KbDocument> existing;

        if (scope == KbScope.USER) {
            existing = documentRepo.findByTitleAndScopeAndUserId(title, scope, userId);
        } else {
            existing = documentRepo.findByTitleAndScopeAndUserIdIsNull(title, scope);
        }

        if (existing.isPresent()) {
            // Update existing document
            KbDocument doc = existing.get();
            doc.setSource(source);
            doc.setContent(content);
            doc.setTags(tags != null ? tags.toArray(new String[0]) : null);
            doc.setVersion(version);
            return documentRepo.save(doc);
        } else {
            // Create new document
            KbDocument doc = KbDocument.create(
                    scope,
                    userId,
                    source,
                    title,
                    content,
                    tags != null ? tags.toArray(new String[0]) : null,
                    version);
            return documentRepo.save(doc);
        }
    }

    /**
     * Re-index an existing document (regenerate chunks and embeddings).
     */
    @Transactional
    public void reindexDocument(UUID documentId) {
        KbDocument document = documentRepo.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        log.info("Re-indexing document {}", documentId);

        // Re-ingest with same parameters
        ingestDocument(
                document.getScope(),
                document.getUserId(),
                document.getSource(),
                document.getTitle(),
                document.getContent(),
                document.getTags() != null ? List.of(document.getTags()) : List.of(),
                document.getVersion());
    }

    /**
     * Delete a document and all its chunks.
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document {} and its chunks", documentId);

        // Chunks will be cascade deleted due to FK constraint
        documentRepo.deleteById(documentId);

        log.info("Deleted document {}", documentId);
    }

    /**
     * Get chunk count for a document.
     */
    public long getChunkCount(UUID documentId) {
        return chunkRepo.countByDocumentId(documentId);
    }
}
