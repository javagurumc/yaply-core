package ai.yaply.controller;

import ai.yaply.entity.Profile;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.service.ConversationService;
import ai.yaply.testsupport.ClarityWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ClarityWebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private ProfileRepository profileRepository;

    @Test
    @WithMockUser(username = "user@example.com")
    void givenAuthenticatedUser_whenCreateConversation_thenReturnsConversationId() {
        // given
        var email = "user@example.com";
        var userId = "user-123";
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        var profile = mock(Profile.class);
        when(profile.getUserId()).thenReturn(userId);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(profile));
        when(conversationService.createConversation(userId)).thenReturn(conversationId);

        // when
        var response = restTestClient.post()
                .uri("/api/conversations")
                .exchange();

        // then
        response
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.conversationId").isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        verify(profileRepository).findByEmail(email);
        verify(conversationService).createConversation(userId);
    }

    @Test
    void givenNoAuthentication_whenCreateConversation_thenForbidden() {
        // when
        var response = restTestClient.post()
                .uri("/api/conversations")
                .exchange();

        // then
        response.expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void givenAuthenticatedUserAndMissingProfile_whenCreateConversation_thenServerError() {
        // given
        when(profileRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        // when
        var response = restTestClient.post()
                .uri("/api/conversations")
                .exchange();

        // then
        response.expectStatus().is5xxServerError();
        verify(profileRepository).findByEmail("user@example.com");
    }

    @Test
    @WithMockUser
    void givenTranscriptBatch_whenIngestEvents_thenDelegatesToService() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // when
        var response = restTestClient.post()
                .uri("/api/conversations/{id}/events", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "conversationId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                          "items": [
                            {
                              "ts": "2024-01-01T00:00:00Z",
                              "role": "user",
                              "type": "text",
                              "content": "hello"
                            }
                          ]
                        }
                        """)
                .exchange();

        // then
        response.expectStatus().isOk();

        verify(conversationService).appendEvents(argThat(req ->
                conversationId.equals(req.conversationId())
                        && req.items() != null
                        && req.items().size() == 1
                        && Instant.parse("2024-01-01T00:00:00Z").equals(req.items().getFirst().ts())
                        && "user".equals(req.items().getFirst().role())
                        && "text".equals(req.items().getFirst().type())
                        && "hello".equals(req.items().getFirst().content())
        ));
    }

    @Test
    void givenNoAuthentication_whenIngestEvents_thenForbidden() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // when
        var response = restTestClient.post()
                .uri("/api/conversations/{id}/events", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "conversationId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                          "items": []
                        }
                        """)
                .exchange();

        // then
        response.expectStatus().isForbidden();
    }

    @Test
    @WithMockUser
    void givenInvalidTranscriptBatch_whenIngestEvents_thenBadRequest() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // when
        var response = restTestClient.post()
                .uri("/api/conversations/{id}/events", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "conversationId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                        }
                        """)
                .exchange();

        // then
        response.expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void givenAuthenticatedUser_whenEndConversation_thenReturnsSummary() {
        // given
        var email = "user@example.com";
        var userId = "user-123";
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var summary = Map.<String, Object>of(
                "summary", "done",
                "takeaways", List.of(Map.of("title", "t1", "detail", "d1"))
        );

        var profile = mock(Profile.class);
        when(profile.getUserId()).thenReturn(userId);
        when(profileRepository.findByEmail(email)).thenReturn(Optional.of(profile));
        when(conversationService.endAndSummarize(conversationId, userId)).thenReturn(summary);

        // when
        var response = restTestClient.post()
                .uri("/api/conversations/{id}/end", conversationId)
                .exchange();

        // then
        response
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.conversationId").isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .jsonPath("$.summary.summary").isEqualTo("done")
                .jsonPath("$.summary.takeaways[0].title").isEqualTo("t1");

        verify(profileRepository).findByEmail(email);
        verify(conversationService).endAndSummarize(conversationId, userId);
    }

    @Test
    void givenNoAuthentication_whenEndConversation_thenForbidden() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // when
        var response = restTestClient.post()
                .uri("/api/conversations/{id}/end", conversationId)
                .exchange();

        // then
        response.expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void givenAuthenticatedUserAndMissingProfile_whenEndConversation_thenServerError() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(profileRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        // when
        var response = restTestClient.post()
                .uri("/api/conversations/{id}/end", conversationId)
                .exchange();

        // then
        response.expectStatus().is5xxServerError();
        verify(profileRepository).findByEmail("user@example.com");
    }

    @Test
    @WithMockUser
    void givenConversationId_whenGetConversation_thenReturnsView() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(conversationService.getConversationView(conversationId)).thenReturn(Map.of(
                "conversationId", conversationId,
                "status", "STARTED"
        ));

        // when
        var response = restTestClient.get()
                .uri("/api/conversations/{id}", conversationId)
                .exchange();

        // then
        response
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.conversationId").isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .jsonPath("$.status").isEqualTo("STARTED");

        verify(conversationService).getConversationView(conversationId);
    }

    @Test
    void givenNoAuthentication_whenGetConversation_thenForbidden() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // when
        var response = restTestClient.get()
                .uri("/api/conversations/{id}", conversationId)
                .exchange();

        // then
        response.expectStatus().isForbidden();
    }

    @Test
    @WithMockUser
    void givenServiceThrows_whenGetConversation_thenServerError() {
        // given
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(conversationService.getConversationView(conversationId))
                .thenThrow(new IllegalArgumentException("Conversation not found"));

        // when
        var response = restTestClient.get()
                .uri("/api/conversations/{id}", conversationId)
                .exchange();

        // then
        response.expectStatus().is5xxServerError();
        verify(conversationService).getConversationView(conversationId);
    }
}
