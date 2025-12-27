package ai.claritywalk.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TranscriptBatchRequest(
        @NotNull UUID conversationId,
        @NotNull List<Item> items
) {
    public record Item(
            @NotNull Instant ts,
            @NotNull String role,   // user|assistant|tool|system
            @NotNull String type,   // text|transcript|tool_output
            @NotNull String content
    ) {}
}
