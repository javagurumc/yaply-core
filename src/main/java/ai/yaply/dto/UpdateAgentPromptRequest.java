package ai.yaply.dto;

import jakarta.validation.constraints.*;

public record UpdateAgentPromptRequest(@NotNull String prompt, @NotNull @PositiveOrZero Long expectedRevision) {
    public UpdateAgentPromptRequest {
        prompt = prompt == null ? null : prompt.strip();
    }
}
