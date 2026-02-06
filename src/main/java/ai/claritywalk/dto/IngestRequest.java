package ai.claritywalk.dto;

import ai.claritywalk.entity.KbScope;

import java.util.List;

/**
 * Request DTO for ingesting a document into the knowledge base.
 */
public record IngestRequest(
        KbScope scope,
        String userId, // required for USER scope
        String source,
        String title,
        String content,
        List<String> tags,
        Integer version) {
    public IngestRequest {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        if (scope == KbScope.USER && (userId == null || userId.isBlank())) {
            throw new IllegalArgumentException("userId is required for USER scope");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (version == null) {
            version = 1;
        }
    }
}
