package ai.yaply.controller;

import ai.yaply.dto.CreateProfileResponse;
import ai.yaply.dto.ProfileResponse;
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
  void givenAuthenticatedUserAndResponses_whenCreateProfile_thenReturnsCreatedProfileIds() {
    var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    when(profileService.createProfile(any(), any())).thenReturn(new CreateProfileResponse(
        id,
        "user-123",
        "user@example.com"));

    restTestClient.post()
        .uri("/api/profile")
        .contentType(MediaType.APPLICATION_JSON)
        .body("""
            {
              "responses": {
                "struggle": "focus",
                "tone": "direct"
              }
            }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.id").isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        .jsonPath("$.userId").isEqualTo("user-123")
        .jsonPath("$.email").isEqualTo("user@example.com");

    verify(profileService).createProfile(any(),
        argThat(auth -> auth.getPrincipal() instanceof UserDetails userDetails &&
            userDetails.getUsername().equals("user@example.com")));
  }
}
