package ai.claritywalk.controller;

import ai.claritywalk.dto.CreateConversationResponse;
import ai.claritywalk.dto.EndConversationResponse;
import ai.claritywalk.dto.TranscriptBatchRequest;
import ai.claritywalk.service.ConversationService;
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

    @PostMapping
    public CreateConversationResponse create(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        UUID id = conversationService.createConversation(userId);
        return new CreateConversationResponse(id);
    }

    @PostMapping("/{id}/events")
    public void ingest(@PathVariable("id") UUID id, @RequestBody @Valid TranscriptBatchRequest req, Authentication auth) {
        // in prod: verify auth user is allowed to write to this conversation
        conversationService.appendEvents(req);
    }

    @PostMapping("/{id}/end")
    public EndConversationResponse end(@PathVariable("id") UUID id, Authentication auth) {
        // in prod: verify auth user owns conversation
        var summary = conversationService.endAndSummarize(id, (String) auth.getPrincipal());
        return new EndConversationResponse(id, summary);
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable("id") UUID id) {
        return conversationService.getConversationView(id);
    }
}