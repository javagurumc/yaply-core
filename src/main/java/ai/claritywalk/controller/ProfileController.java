package ai.claritywalk.controller;

import ai.claritywalk.dto.CreateProfileRequest;
import ai.claritywalk.dto.CreateProfileResponse;
import ai.claritywalk.dto.ProfileResponse;
import ai.claritywalk.service.ProfileService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse getProfile(Authentication auth) {
        return profileService.getProfile(auth);
    }

    @PostMapping
    public CreateProfileResponse create(@RequestBody CreateProfileRequest request, Authentication auth) {
        return profileService.createProfile(request, auth);
    }

}
