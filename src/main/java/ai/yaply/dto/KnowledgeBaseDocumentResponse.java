package ai.yaply.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeBaseDocumentResponse(
        UUID id,
        String title,
        String content,
        String source,
        List<String> tags,
        Integer version,
        Long chunkCount,
        Instant updatedAt
) {
}
