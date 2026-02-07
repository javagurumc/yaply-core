package ai.claritywalk.controller;

import ai.claritywalk.dto.CreateProfileResponse;
import ai.claritywalk.dto.ProfileResponse;
import ai.claritywalk.service.ProfileService;
import ai.claritywalk.testsupport.ClarityWebMvcTest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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

    @Test
    void getProfile_returnsProfileResponse() {
        var email = "user@example.com";
        var basicAuth = getBasicAuth(email);

        var createdAt = Instant.parse("2024-01-01T00:00:00Z");
        when(profileService.getProfile(any())).thenReturn(new ProfileResponse(
                email,
                Map.of("struggle", "focus"),
                createdAt
        ));

        restTestClient.get().uri("/api/profile")
                .header(HttpHeaders.AUTHORIZATION, basicAuth)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.responses.struggle").isEqualTo("focus")
                .jsonPath("$.createdAt").isEqualTo("2024-01-01T00:00:00Z");

        //todo: add actual argument matching to verify the principal passed to profileService.getProfile has the expected email
        verify(profileService).getProfile(any());

    }

    private @NonNull String getBasicAuth(String username) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":pw").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void create_updatesProfileAndReturnsIds() {
        var user = User.withUsername("user@example.com").password("pw").roles("USER").build();
        var basic = getBasicAuth(user.getUsername());

        var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(profileService.createProfile(any(), any())).thenReturn(new CreateProfileResponse(
                id,
                "user-123",
                "user@example.com"
        ));

        restTestClient.post()
                .uri("/api/profile")
                .header(HttpHeaders.AUTHORIZATION, basic)
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

        verify(profileService).createProfile(any(), any());
    }
}
