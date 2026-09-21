package ai.yaply.dto;
import java.time.Instant;
import java.util.UUID;
public record AgentResponse(UUID id, String name, String description, Instant createdAt, Instant updatedAt, boolean promptConfigured) {}
