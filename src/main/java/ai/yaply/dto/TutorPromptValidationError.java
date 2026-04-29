package ai.yaply.dto;

import java.util.List;

public record TutorPromptValidationError(
        String message,
        List<String> details
) {
}

