package ai.yaply.service;

import ai.yaply.dto.CreateProfileRequest;
import ai.yaply.entity.Profile;
import ai.yaply.repo.ProfileRepository;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    private ProfileRepository profileRepository;
    
    private ValidateTutorPromptService validateTutorPromptService;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        validateTutorPromptService = mock(ValidateTutorPromptService.class);
        profileService = new ProfileService(profileRepository, new ObjectMapper(), validateTutorPromptService);
    }

    @Test
    void givenAuthenticatedUser_whenGetProfile_thenReturnsProfileResponse() {
        // Given
        var email = "user@example.com";
        var responsesJson = "{\"struggle\":\"focus\"}";
        var profile = Profile.builder()
                .email(email)
                .responses(responsesJson)
                .build();

        var authentication = mockAuthentication(email);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(profile));

        // When
        var response = profileService.getProfile(authentication);

        // Then
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.responses()).containsEntry("struggle", "focus");
    }

    @Test
    void givenAuthenticatedUser_whenGetProfile_andProfileNotFound_thenThrowsException() {
        // Given
        var email = "user@example.com";
        var authentication = mockAuthentication(email);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> profileService.getProfile(authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile not found for user: " + email);
    }

    @Test
    void givenAuthenticatedUserAndRequest_whenCreateProfile_thenSavesAndReturnsResponse() {
        // Given
        var email = "user@example.com";
        var id = UUID.randomUUID();
        var userId = "user-123";
        var request = new CreateProfileRequest(Map.of("struggle", "focus", "tone", "direct"));
        var existingProfile = Profile.builder()
                .id(id)
                .userId(userId)
                .email(email)
                .responses("{}")
                .build();

        var authentication = mockAuthentication(email);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var response = profileService.createProfile(request, authentication);

        // Then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(email);

        verify(profileRepository).save(argThat(profile ->
                        profile.getId().equals(id)
                        && profile.getUserId().equals(userId)
                        && profile.getEmail().equals(email)
                        && profile.getResponses().contains("focus")
                        && profile.getResponses().contains("direct")));
    }

    @Test
    void givenAuthenticatedUserAndRequest_whenCreateProfile_andProfileNotFound_thenThrowsException() {
        // Given
        var email = "user@example.com";
        var request = new CreateProfileRequest(Map.of("struggle", "focus"));

        var authentication = mockAuthentication(email);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> profileService.createProfile(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile not found for user: " + email);
    }

    private @NonNull Authentication mockAuthentication(String email) {
        var authentication = mock(Authentication.class);
        var userDetails = mock(UserDetails.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        return authentication;
    }
}