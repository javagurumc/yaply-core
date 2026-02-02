package ai.claritywalk.validation;

import ai.claritywalk.dto.RegisterRequest;
import ai.claritywalk.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator that checks if an email is already registered.
 */
@Component
@RequiredArgsConstructor
public class EmailUniqueValidator implements RegistrationValidator {

    private final ProfileRepository profileRepository;

    @Override
    public void validate(RegisterRequest request) {
        if (profileRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
    }
}
