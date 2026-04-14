package ai.yaply.controller;

import ai.yaply.dto.CreateConversationResponse;
import ai.yaply.dto.EndConversationResponse;
import ai.yaply.dto.TranscriptBatchRequest;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.service.ConversationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
        //Todo move this check to the service
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        String userId = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getUserId();

        UUID id = conversationService.createConversation(userId);
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
        // in prod: verify auth user owns conversation
        //Todo move this check to the service
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        String userId = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getUserId();
        var summary = conversationService.endAndSummarize(id, userId);
        return new EndConversationResponse(id, summary);
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable("id") UUID id) {
        return conversationService.getConversationView(id);
    }
}