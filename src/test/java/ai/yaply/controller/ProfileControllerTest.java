package ai.yaply.controller;

import ai.yaply.dto.*;
import ai.yaply.service.ProfileService;
import ai.yaply.testsupport.ClarityWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ClarityWebMvcTest(ProfileController.class)
class ProfileControllerTest {

  @Autowired
  private RestTestClient restTestClient;

  @MockitoBean
  private ProfileService profileService;

  @WithMockUser(username = "user@example.com")
  @Test
  void givenAuthenticatedUser_whenGetProfile_thenReturnsProfileResponse() {
    var email = "user@example.com";

    var createdAt = Instant.parse("2024-01-01T00:00:00Z");
    when(profileService.getProfile(any())).thenReturn(new ProfileResponse(
        email,
        Map.of("struggle", "focus"),
        createdAt));

    restTestClient.get().uri("/api/profile")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.email").isEqualTo(email)
        .jsonPath("$.responses.struggle").isEqualTo("focus")
        .jsonPath("$.createdAt").isEqualTo("2024-01-01T00:00:00Z");

    verify(profileService)
        .getProfile(argThat(auth -> auth.getPrincipal() instanceof UserDetails userDetails &&
            userDetails.getUsername().equals(email)));
  }

  @Test
  @WithMockUser(username = "user@example.com")
  void givenAuthenticatedUser_whenGetTutorPrompt_thenReturnsCurrentPrompt() {
    String customPrompt = "You are a patient math tutor who explains step by step.";
    when(profileService.getCustomPrompt(any())).thenReturn(new GetTutorPromptResponse(customPrompt));

    restTestClient.get().uri("/api/profile/tutor-prompt")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.prompt").isEqualTo(customPrompt);

    verify(profileService)
        .getCustomPrompt(argThat(auth -> auth.getPrincipal() instanceof UserDetails userDetails &&
            userDetails.getUsername().equals("user@example.com")));
  }

  @Test
  void givenAnonymousUser_whenGetTutorPrompt_thenRequestIsRejected() {
    restTestClient.get().uri("/api/profile/tutor-prompt")
        .exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  void givenAnonymousUser_whenUpdateTutorPrompt_thenRequestIsRejected() {
    restTestClient.put().uri("/api/profile/tutor-prompt")
        .contentType(MediaType.APPLICATION_JSON)
        .body("""
                {
                  "prompt": "be concise"
                }
                """)
        .exchange()
        .expectStatus().is4xxClientError();
  }

  // Note: POST and PUT endpoints to /api/profile are tested via integration tests
  // The unit tests above verify the GET endpoints with @WithMockUser annotation.
  // For full validation of request/response serialization and error handling,
  // see integration tests in docker-compose CI pipeline.
}
