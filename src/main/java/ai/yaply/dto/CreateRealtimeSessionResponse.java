package ai.yaply.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateRealtimeSessionResponse(
        UUID conversationId,
        String clientSecret,     // ephemeral key returned by OpenAI
        Instant expiresAt,
        String model
) {}
