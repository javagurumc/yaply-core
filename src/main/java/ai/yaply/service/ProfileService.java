package ai.yaply.service;

import ai.yaply.dto.*;
import ai.yaply.entity.Profile;
import ai.yaply.repo.ProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final ValidateTutorPromptService validateTutorPromptService;

    public ProfileResponse getProfile(Authentication auth) {
        var email = getEmail(auth);
        var profile = findProfileByEmail(email);

        Map<String, String> responsesMap = objectMapper.readValue(
                profile.getResponses(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        return new ProfileResponse(
                profile.getEmail(),
                responsesMap,
                Instant.now() // Using current time since createdAt is not in the entity
        );
    }

    public CreateProfileResponse createProfile(CreateProfileRequest request, Authentication auth) {
        var email = getEmail(auth);
        var profile = findProfileByEmail(email);

        var responsesJson = objectMapper.writeValueAsString(request.responses());
        profile.setResponses(responsesJson);

        var updatedProfile = profileRepository.save(profile);

        return new CreateProfileResponse(
                updatedProfile.getId(),
                updatedProfile.getUserId(),
                updatedProfile.getEmail());
    }

    private @NonNull Profile findProfileByEmail(String email) {
        var profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + email));
        return profile;
    }

    private @NonNull String getEmail(Authentication auth) {
        return ((UserDetails) auth.getPrincipal()).getUsername();
    }

    public UpdateTutorPromptResponse updateCustomPrompt(UpdateTutorPromptRequest request, Authentication auth) {
        var email = getEmail(auth);
        var profile = findProfileByEmail(email);

        // Validate the prompt (empty prompts are allowed for reset)
        var validationError = validateTutorPromptService.validate(request.prompt());
        if (validationError.isPresent()) {
            throw new IllegalArgumentException("Prompt validation failed: " + validationError.get().message());
        }

        // Update the prompt
        profile.setCustomTutorPrompt(request.prompt());
        var updatedProfile = profileRepository.save(profile);

        String message = request.prompt().trim().isEmpty() 
                ? "Tutor prompt reset to default" 
                : "Tutor prompt updated successfully";

        log.info("Custom tutor prompt updated for user: {}", email);

        return new UpdateTutorPromptResponse(updatedProfile.getCustomTutorPrompt(), message);
    }

    public GetTutorPromptResponse getCustomPrompt(Authentication auth) {
        var email = getEmail(auth);
        var profile = findProfileByEmail(email);
        return new GetTutorPromptResponse(profile.getCustomTutorPrompt());
    }

}
