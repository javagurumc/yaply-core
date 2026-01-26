package ai.claritywalk.service;

import ai.claritywalk.dto.ToolExecuteRequest;
import ai.claritywalk.dto.ToolExecuteResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ToolExecutionService {

    private final ExternalApiFacade externalApi;

    public ToolExecuteResponse execute(ToolExecuteRequest req) {
        // Allowlist tools
        return switch (req.toolName()) {
            case "get_user_profile" -> new ToolExecuteResponse(req.toolName(), externalApi.getUserProfile(req.arguments()));
            case "get_todays_plan" -> new ToolExecuteResponse(req.toolName(), externalApi.getTodaysPlan(req.arguments()));
            case "get_weather" -> new ToolExecuteResponse(req.toolName(), externalApi.getWeather(req.arguments()));
            default -> throw new IllegalArgumentException("Unknown tool: " + req.toolName());
        };
    }

}
