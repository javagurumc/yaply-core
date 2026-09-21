package ai.yaply.dto;
import java.time.Instant;
import java.util.UUID;
public record AgentPromptResponse(UUID agentId, String prompt, long revision, boolean configured, Instant updatedAt) {}
