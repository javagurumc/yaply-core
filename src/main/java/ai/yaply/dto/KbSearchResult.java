package ai.yaply.dto;

import java.util.List;
import java.util.UUID;

/**
 * Knowledge Base search result.
 */
public record KbSearchResult(
        List<ChunkResult> results,
        String usageRule) {
    public static KbSearchResult create(List<ChunkResult> results) {
        return new KbSearchResult(
                results,
                "Use these snippets as grounded context. If insufficient, ask one clarifying question.");
    }

    /**
     * Individual chunk result with metadata.
     */
    public record ChunkResult(
            UUID chunkId,
            UUID documentId,
            String title,
            String source,
            String text,
            double score) {
    }
}
