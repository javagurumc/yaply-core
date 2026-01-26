package ai.claritywalk.dto;

import java.util.Map;

public record CreateProfileRequest(String email, Map<String, String> responses) {
}
