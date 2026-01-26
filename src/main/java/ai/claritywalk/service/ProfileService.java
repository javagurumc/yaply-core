package ai.claritywalk.service;

import ai.claritywalk.dto.CreateProfileRequest;
import ai.claritywalk.dto.CreateProfileResponse;
import ai.claritywalk.entity.Profile;
import ai.claritywalk.repo.ProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public CreateProfileResponse createProfile(CreateProfileRequest request, Authentication auth) {

        var userId = auth.getPrincipal().toString();
        var responses = request.responses().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));;

        var profile = Profile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .email(request.email())
                .responses(responses)
                .build();

        var createdProfile = profileRepository.save(profile);
        return new CreateProfileResponse(createdProfile.getId(), createdProfile.getUserId(), createdProfile.getEmail());
    }

}
