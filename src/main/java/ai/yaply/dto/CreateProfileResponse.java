package ai.yaply.dto;

import java.util.UUID;

public record CreateProfileResponse(UUID id, String userId, String email) {
}
