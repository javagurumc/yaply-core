package ai.yaply.dto;

import java.util.List;

public record UpdateKnowledgeBaseDocumentRequest(
        String title,
        String content,
        List<String> tags,
        String source,
        Integer version
) {
}
