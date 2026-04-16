package ai.yaply.controller;

import ai.yaply.dto.CreateRealtimeSessionResponse;
import ai.yaply.entity.Profile;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.service.OpenAIRealtimeService;
import ai.yaply.testsupport.ClarityWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ClarityWebMvcTest(RealtimeSessionController.class)
class RealtimeSessionControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private OpenAIRealtimeService realtimeService;

    @MockitoBean
    private ProfileRepository profileRepository;

    @Test
    void givenNoAuthentication_whenCreateSession_thenReturnsEphemeralToken() {
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var expiresAt = Instant.parse("2026-04-17T12:00:00Z");

        when(realtimeService.createEphemeralSession(
                new ai.yaply.dto.CreateRealtimeSessionRequest(conversationId, "alloy", "en"),
                null
        )).thenReturn(new CreateRealtimeSessionResponse(conversationId, "secret", expiresAt, "gpt-realtime"));

        restTestClient.post()
                .uri("/api/realtime/session")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "conversationId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                          "voice": "alloy",
                          "language": "en"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.conversationId").isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .jsonPath("$.clientSecret").isEqualTo("secret");

        verify(realtimeService).createEphemeralSession(
                new ai.yaply.dto.CreateRealtimeSessionRequest(conversationId, "alloy", "en"),
                null
        );
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void givenAuthenticatedUser_whenCreateSession_thenUsesPrincipalName() {
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var expiresAt = Instant.parse("2026-04-17T12:00:00Z");
        var profile = mock(Profile.class);
        when(profile.getUserId()).thenReturn("user-123");
        when(profileRepository.findByEmail("user@example.com")).thenReturn(Optional.of(profile));

        when(realtimeService.createEphemeralSession(
                new ai.yaply.dto.CreateRealtimeSessionRequest(conversationId, "alloy", "en"),
                "user-123"
        )).thenReturn(new CreateRealtimeSessionResponse(conversationId, "secret", expiresAt, "gpt-realtime"));

        restTestClient.post()
                .uri("/api/realtime/session")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "conversationId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                          "voice": "alloy",
                          "language": "en"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();

        verify(realtimeService).createEphemeralSession(
                new ai.yaply.dto.CreateRealtimeSessionRequest(conversationId, "alloy", "en"),
                "user-123"
        );
    }
}
