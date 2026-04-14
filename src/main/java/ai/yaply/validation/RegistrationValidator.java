package ai.yaply.validation;

import ai.yaply.dto.RegisterRequest;

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
