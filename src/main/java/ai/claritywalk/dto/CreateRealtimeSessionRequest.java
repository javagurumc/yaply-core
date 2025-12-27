package ai.claritywalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRealtimeSessionRequest(
        @NotNull UUID conversationId,
        @NotBlank String voice,
        @NotBlank String language
) {}
