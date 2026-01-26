package ai.claritywalk.dto;

import java.util.UUID;

public record CreateProfileResponse(UUID id, String userId, String email) {
}
