package ai.claritywalk.service;

import ai.claritywalk.dto.CreateProfileRequest;
import ai.claritywalk.dto.CreateProfileResponse;
import ai.claritywalk.dto.ProfileResponse;
import ai.claritywalk.entity.Profile;
import ai.claritywalk.repo.ProfileRepository;
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

}
