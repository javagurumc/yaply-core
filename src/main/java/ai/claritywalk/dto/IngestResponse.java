package ai.claritywalk.dto;

import java.util.UUID;

/**
 * Response DTO for document ingestion.
 */
public record IngestResponse(
        UUID documentId,
        int chunksCreated,
        String message) {
    public static IngestResponse success(UUID documentId, int chunksCreated) {
        return new IngestResponse(
                documentId,
                chunksCreated,
                "Document ingested successfully with " + chunksCreated + " chunks");
    }
}
