package ai.yaply.controller;

import ai.yaply.dto.*;
import ai.yaply.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService service;
    @GetMapping("/{id}/prompt")
    public AgentPromptResponse getPrompt(@PathVariable UUID id, Authentication auth) {
        return service.getPrompt(id, auth);
    }

    @PutMapping("/{id}/prompt")
    public AgentPromptResponse updatePrompt(@PathVariable UUID id, @Valid @RequestBody UpdateAgentPromptRequest request, Authentication auth) {
        return service.updatePrompt(id, request, auth);
    }

    @GetMapping public List<AgentResponse> list(Authentication auth) { return service.list(auth); }
    @GetMapping("/{id}") public AgentResponse get(@PathVariable UUID id, Authentication auth) { return service.get(id, auth); }
    @PostMapping public ResponseEntity<AgentResponse> create(@Valid @RequestBody CreateAgentRequest request, Authentication auth) {
        var agent = service.create(request, auth);
        return ResponseEntity.created(URI.create("/api/agents/" + agent.id())).body(agent);
    }
    @PutMapping("/{id}") public AgentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAgentRequest request, Authentication auth) {
        return service.update(id, request, auth);
    }
}
