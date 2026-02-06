package ai.claritywalk.controller;

import ai.claritywalk.dto.IngestRequest;
import ai.claritywalk.dto.IngestResponse;
import ai.claritywalk.service.KbIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin controller for knowledge base management.
 * Provides endpoints for document ingestion, re-indexing, and deletion.
 */
@Slf4j
@RestController
@RequestMapping("/api/kb/admin")
@RequiredArgsConstructor
public class KbAdminController {

    private final KbIngestService ingestService;

    /**
     * Ingest a new document into the knowledge base.
     * POST /api/kb/admin/documents
     */
    @PostMapping("/documents")
    public ResponseEntity<IngestResponse> ingestDocument(@RequestBody IngestRequest request) {
        log.info("Ingesting document: scope={}, title={}", request.scope(), request.title());

        UUID documentId = ingestService.ingestDocument(
                request.scope(),
                request.userId(),
                request.source(),
                request.title(),
                request.content(),
                request.tags(),
                request.version() != null ? request.version() : 1);

        // Get chunk count
        long chunkCount = ingestService.getChunkCount(documentId);

        return ResponseEntity.ok(IngestResponse.success(documentId, (int) chunkCount));
    }

    /**
     * Re-index an existing document (regenerate chunks and embeddings).
     * POST /api/kb/admin/documents/{id}/reindex
     */
    @PostMapping("/documents/{id}/reindex")
    public ResponseEntity<Void> reindexDocument(@PathVariable UUID id) {
        log.info("Re-indexing document: {}", id);

        ingestService.reindexDocument(id);

        return ResponseEntity.ok().build();
    }

    /**
     * Delete a document and all its chunks.
     * DELETE /api/kb/admin/documents/{id}
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        log.info("Deleting document: {}", id);

        ingestService.deleteDocument(id);

        return ResponseEntity.ok().build();
    }
}
