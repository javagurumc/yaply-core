package ai.yaply.controller;

import ai.yaply.dto.*;
import ai.yaply.service.ProfileService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/tutor-prompt")
    public GetTutorPromptResponse getTutorPrompt(Authentication auth) {
        return profileService.getCustomPrompt(auth);
    }

    @PutMapping("/tutor-prompt")
    public ResponseEntity<?> updateTutorPrompt(@RequestBody UpdateTutorPromptRequest request, Authentication auth) {
        try {
            var response = profileService.updateCustomPrompt(request, auth);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new TutorPromptValidationError(
                    "Validation Error",
                    java.util.List.of(e.getMessage())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new TutorPromptValidationError(
                    "Error updating tutor prompt",
                    java.util.List.of(e.getMessage())
            ));
        }
    }

}
