package ai.claritywalk.dto;

import java.util.Map;
import java.util.UUID;

public record EndConversationResponse(
        UUID conversationId,
        Map<String, Object> summary
) {}
