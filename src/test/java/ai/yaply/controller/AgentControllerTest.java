package ai.yaply.controller;

import ai.yaply.dto.*;
import ai.yaply.service.AgentService;
import ai.yaply.testsupport.ClarityWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ClarityWebMvcTest(AgentController.class)
class AgentControllerTest {
    @Autowired RestTestClient client;
    @MockitoBean AgentService service;
    private final UUID id = UUID.randomUUID();
    private AgentResponse response() { return new AgentResponse(id, "Math", "", Instant.now(), Instant.now()); }

    @Test void anonymousRequestsAreUnauthorized() {
        client.get().uri("/api/agents").exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/agents").contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "Math")).exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/agents/" + id).exchange().expectStatus().isUnauthorized();
        client.put().uri("/api/agents/" + id).contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "Math")).exchange().expectStatus().isUnauthorized();
        verifyNoInteractions(service);
    }
    @Test @WithMockUser(username = "teacher@example.com")
    void createsWithLocationAndIgnoresSubmittedOwner() {
        when(service.create(any(), any())).thenReturn(response());
        client.post().uri("/api/agents").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", " Math ", "ownerProfileId", UUID.randomUUID().toString()))
                .exchange().expectStatus().isCreated().expectHeader().valueEquals("Location", "/api/agents/" + id)
                .expectBody().jsonPath("$.id").isEqualTo(id.toString()).jsonPath("$.ownerProfileId").doesNotExist();
        verify(service).create(eq(new CreateAgentRequest("Math", "")), argThat(a -> a.getName().equals("teacher@example.com")));
    }
    @Test @WithMockUser
    void listsReadsAndUpdates() {
        when(service.list(any())).thenReturn(List.of(response()));
        when(service.get(eq(id), any())).thenReturn(response());
        when(service.update(eq(id), any(), any())).thenReturn(response());
        client.get().uri("/api/agents").exchange().expectStatus().isOk().expectBody().jsonPath("$[0].id").isEqualTo(id.toString());
        client.get().uri("/api/agents/" + id).exchange().expectStatus().isOk();
        client.put().uri("/api/agents/" + id).contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "Math")).exchange().expectStatus().isOk();
    }
    @Test @WithMockUser
    void validatesMetadataAndMalformedIds() {
        for (var body : List.of(Map.of("name", "  "), Map.of("name", "x".repeat(121)), Map.of("name", "Math", "description", "x".repeat(2001)), Map.of("description", "Missing name"))) {
            client.post().uri("/api/agents").contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.message").exists();
            client.put().uri("/api/agents/" + id).contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isBadRequest();
        }
        client.get().uri("/api/agents/not-a-uuid").exchange().expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }
    @Test @WithMockUser
    void foreignOrMissingAgentIsNotFound() {
        when(service.get(eq(id), any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
        when(service.update(eq(id), any(), any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
        client.get().uri("/api/agents/" + id).exchange().expectStatus().isNotFound();
        client.put().uri("/api/agents/" + id).contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "Math")).exchange().expectStatus().isNotFound();
    }
}
