package ai.claritywalk.dto;

import java.util.Map;

public record ToolExecuteResponse(
        String toolName,
        Map<String, Object> result
) {}
