package ai.yaply.service;

import ai.yaply.dto.KbSearchResult;
import ai.yaply.dto.ToolExecuteRequest;
import ai.yaply.dto.ToolExecuteResponse;
import ai.yaply.entity.KbScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class ToolExecutionService {

    private final ExternalApiFacade externalApi;
    private final KbSearchService kbSearchService;
    private final KbIngestService kbIngestService;
    private final ObjectMapper objectMapper;

    public ToolExecuteResponse execute(ToolExecuteRequest req) {
        // Allowlist tools
        return switch (req.toolName()) {
            case KB_SEARCH -> executeKbSearch(req);
            case KB_WRITE_MEMORY -> executeKbWriteMemory(req);
            default -> throw new IllegalArgumentException("Unknown tool: " + req.toolName());
        };
    }

    /**
     * Execute KB search tool.
     * Searches the knowledge base for relevant information.
     */
    private ToolExecuteResponse executeKbSearch(ToolExecuteRequest req) {
        try {
            Map<String, Object> args = req.arguments();

            String query = (String) args.get("query");
            String scopeStr = (String) args.get("scope");
            Integer topK = args.containsKey("top_k") ? ((Number) args.get("top_k")).intValue() : null;
            String source = (String) args.get("source");
            String userId = (String) args.get("userId"); // Passed from conversation context

            KbScope scope = KbScope.valueOf(scopeStr);

            KbSearchResult result = kbSearchService.search(query, scope, userId, topK, source);

            // Convert result to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);

            return new ToolExecuteResponse(req.toolName(), resultMap);

        } catch (Exception e) {
            return new ToolExecuteResponse(req.toolName(),
                    Map.of("error", e.getMessage()));
        }
    }

    /**
     * Execute KB write memory tool.
     * Saves information to the user's personal memory.
     */
    private ToolExecuteResponse executeKbWriteMemory(ToolExecuteRequest req) {
        try {
            Map<String, Object> args = req.arguments();

            String title = (String) args.get("title");
            String content = (String) args.get("content");
            List<String> tags = args.containsKey("tags") ? (List<String>) args.get("tags") : List.of();
            String userId = (String) args.get("userId"); // Passed from conversation context

            // Ingest as USER scope document
            kbIngestService.ingestDocument(
                    KbScope.USER,
                    userId,
                    "memory",
                    title,
                    content,
                    tags,
                    1);

            return new ToolExecuteResponse(req.toolName(),
                    Map.of("success", true, "message", "Memory '" + title + "' saved successfully"));

        } catch (Exception e) {
            return new ToolExecuteResponse(req.toolName(),
                    Map.of("success", false, "error", e.getMessage()));
        }
    }

}
