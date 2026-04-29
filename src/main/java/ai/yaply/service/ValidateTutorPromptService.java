package ai.yaply.service;

import ai.yaply.dto.TutorPromptValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for validating custom tutor prompts.
 * Enforces character limits and checks for harmful content patterns.
 */
@Slf4j
@Service
public class ValidateTutorPromptService {

    private static final int MIN_PROMPT_LENGTH = 5;
    private static final int MAX_PROMPT_LENGTH = 5000;

    // Patterns for harmful content detection
    private static final List<Pattern> HARMFUL_PATTERNS = List.of(
            Pattern.compile("ignore safety|ignore your instructions|forget.*instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bypass.*security|disable.*safety", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act as.*malicious|pretend.*harmful", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Validates a custom tutor prompt.
     * Returns an Optional containing validation errors if any are found.
     */
    public Optional<TutorPromptValidationError> validate(String prompt) {
        if (prompt == null) {
            return Optional.of(new TutorPromptValidationError(
                    "Prompt validation failed",
                    List.of("Prompt cannot be null")
            ));
        }

        List<String> errors = new ArrayList<>();

        // Check length constraints
        if (prompt.trim().isEmpty()) {
            // Empty prompts are allowed (reset to default)
            return Optional.empty();
        }

        if (prompt.length() < MIN_PROMPT_LENGTH) {
            errors.add(String.format("Prompt must be at least %d characters long", MIN_PROMPT_LENGTH));
        }

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            errors.add(String.format("Prompt cannot exceed %d characters (current: %d)", MAX_PROMPT_LENGTH, prompt.length()));
        }

        // Check for harmful content patterns
        for (Pattern pattern : HARMFUL_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                errors.add("Prompt contains potentially harmful instructions or patterns");
                break;
            }
        }

        if (!errors.isEmpty()) {
            return Optional.of(new TutorPromptValidationError(
                    "Prompt validation failed",
                    errors
            ));
        }

        return Optional.empty();
    }
}

