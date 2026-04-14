package ai.yaply.dto;

import java.time.Instant;
import java.util.Map;

public record ProfileResponse(
        String email,
        Map<String, String> responses,
        Instant createdAt) {
}
