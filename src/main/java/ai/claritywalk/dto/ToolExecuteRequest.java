package ai.claritywalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record ToolExecuteRequest(
        @NotNull UUID conversationId,
        @NotBlank String toolName,
        @NotNull Map<String, Object> arguments
) {}
