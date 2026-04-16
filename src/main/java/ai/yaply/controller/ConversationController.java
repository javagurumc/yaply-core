package ai.yaply.controller;

import ai.yaply.dto.CreateConversationResponse;
import ai.yaply.dto.EndConversationResponse;
import ai.yaply.dto.TranscriptBatchRequest;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.service.ConversationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ProfileRepository profileRepository;

    @PostMapping
    public CreateConversationResponse create(Authentication auth) {
        UUID id = conversationService.createConversation(resolveUserId(auth));
        return new CreateConversationResponse(id);
    }

    @PostMapping("/{id}/events")
    public void ingest(@PathVariable("id") UUID id, @RequestBody @Valid TranscriptBatchRequest req,
                       Authentication auth) {
        // in prod: verify auth user is allowed to write to this conversation
        conversationService.appendEvents(req);
    }

    @PostMapping("/{id}/end")
    public EndConversationResponse end(@PathVariable("id") UUID id, Authentication auth) {
        var summary = conversationService.endAndSummarize(id, resolveUserId(auth));
        return new EndConversationResponse(id, summary);
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable("id") UUID id) {
        return conversationService.getConversationView(id);
    }

    private String resolveUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return profileRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getUserId();
    }
}
