package ai.claritywalk.controller;

import ai.claritywalk.dto.CreateRealtimeSessionRequest;
import ai.claritywalk.dto.CreateRealtimeSessionResponse;
import ai.claritywalk.service.OpenAIRealtimeService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/realtime")
public class RealtimeSessionController {

    private final OpenAIRealtimeService realtimeService;

    @PostMapping("/session")
    public CreateRealtimeSessionResponse createSession(@RequestBody @Valid CreateRealtimeSessionRequest req,
                                                       Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return realtimeService.createEphemeralSession(req, userId);
    }
}
