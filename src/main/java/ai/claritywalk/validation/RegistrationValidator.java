package ai.claritywalk.validation;

import ai.claritywalk.dto.RegisterRequest;

/**
 * Strategy interface for validating registration requests.
 */
public interface RegistrationValidator {

    /**
     * Validates a registration request.
     * 
     * @param request the registration request to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validate(RegisterRequest request);
}
