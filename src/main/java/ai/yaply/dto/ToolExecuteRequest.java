package ai.yaply.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record ToolExecuteRequest(
        @NotNull UUID conversationId,
        @NotNull ToolName toolName,
        @NotNull Map<String, Object> arguments
) {}
