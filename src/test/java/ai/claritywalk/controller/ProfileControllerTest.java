package ai.claritywalk.controller;

import ai.claritywalk.dto.CreateProfileResponse;
import ai.claritywalk.dto.ProfileResponse;
import ai.claritywalk.service.ProfileService;
import ai.claritywalk.testsupport.ClarityWebMvcTest;
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
        var user = User.withUsername("user@example.com").password("pw").roles("USER").build();
        var basic = "Basic " + Base64.getEncoder().encodeToString((user.getUsername() + ":pw")
                .getBytes(StandardCharsets.UTF_8));

        var createdAt = Instant.parse("2024-01-01T00:00:00Z");
        when(profileService.getProfile(any())).thenReturn(new ProfileResponse(
                "user@example.com",
                Map.of("struggle", "focus"),
                createdAt
        ));

        restTestClient.get().uri("/api/profile")
                .header(HttpHeaders.AUTHORIZATION, basic)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.email").isEqualTo("user@example.com")
                .jsonPath("$.responses.struggle").isEqualTo("focus")
                .jsonPath("$.createdAt").isEqualTo("2024-01-01T00:00:00Z");

        verify(profileService).getProfile(any());
    }

    @Test
    void create_updatesProfileAndReturnsIds() {
        var user = User.withUsername("user@example.com").password("pw").roles("USER").build();
        var basic = "Basic " + Base64.getEncoder().encodeToString((user.getUsername() + ":pw")
                .getBytes(StandardCharsets.UTF_8));

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
