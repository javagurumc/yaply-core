package ai.claritywalk.dto;

import java.util.Map;

public record ToolExecuteResponse(
        ToolName toolName,
        Map<String, Object> result
) {}
