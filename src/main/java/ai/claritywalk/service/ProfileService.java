package ai.claritywalk.service;

import ai.claritywalk.dto.CreateProfileRequest;
import ai.claritywalk.dto.CreateProfileResponse;
import ai.claritywalk.dto.ProfileResponse;
import ai.claritywalk.entity.Profile;
import ai.claritywalk.repo.ProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public ProfileResponse getProfile(Authentication auth) {
        // Get email from authenticated user (from JWT token)
        String email = ((UserDetails) auth.getPrincipal()).getUsername();

        // Find profile by email
        Profile profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        // Parse responses JSON to Map
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
        // Get email from authenticated user (from JWT token)
        String email = ((UserDetails) auth.getPrincipal()).getUsername();

        // Find existing profile by email (created during registration)
        Profile profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + email));

        // Update profile with onboarding responses
        String responsesJson = objectMapper.writeValueAsString(request.responses());
        profile.setResponses(responsesJson);

        // Save updated profile
        Profile updatedProfile = profileRepository.save(profile);

        return new CreateProfileResponse(
                updatedProfile.getId(),
                updatedProfile.getUserId(),
                updatedProfile.getEmail());
    }

}
