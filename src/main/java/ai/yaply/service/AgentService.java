package ai.yaply.service;

import ai.yaply.dto.*;
import ai.yaply.entity.ExamAgent;
import ai.yaply.entity.AgentPrompt;
import ai.yaply.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentService {
    private final ExamAgentRepository agents;
    private final CurrentProfileService currentProfile;
    private final AgentPromptRepository prompts;
    private final ValidateTutorPromptService promptValidator;

    private UUID owner(Authentication auth) {
        return currentProfile.id(auth);
    }

    public List<AgentResponse> list(Authentication auth) {
        var owned = agents.findByOwnerProfileIdOrderByCreatedAtDescIdAsc(owner(auth));
        var configured = owned.isEmpty() ? Set.<UUID>of() : prompts.findConfiguredIds(owned.stream().map(ExamAgent::getId).toList());
        return owned.stream().map(a -> response(a, configured.contains(a.getId()))).toList();
    }

    public AgentResponse get(UUID id, Authentication auth) { return response(owned(id, auth)); }

    @Transactional
    public AgentResponse create(CreateAgentRequest request, Authentication auth) {
        UUID ownerId = owner(auth);
        validate(request.name(), request.description());
        var agent = agents.save(new ExamAgent(ownerId, request.name(), request.description()));
        prompts.save(new AgentPrompt(agent.getId()));
        return response(agent, false);
    }

    @Transactional
    public AgentResponse update(UUID id, UpdateAgentRequest request, Authentication auth) {
        ExamAgent agent = owned(id, auth);
        validate(request.name(), request.description());
        agent.updateDetails(request.name(), request.description());
        return response(agents.save(agent));
    }

    private ExamAgent owned(UUID id, Authentication auth) {
        return agents.findByIdAndOwnerProfileId(id, owner(auth)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
    }

    private void validate(String name, String description) {
        if (name == null || name.isBlank() || name.length() > 120 || description.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required (maximum 120 characters); description must not exceed 2000 characters.");
    }

    public AgentPromptResponse getPrompt(UUID id, Authentication auth) {
        owned(id, auth);
        return promptResponse(prompts.findById(id).orElseThrow());
    }

    @Transactional
    public AgentPromptResponse updatePrompt(UUID id, UpdateAgentPromptRequest request, Authentication auth) {
        owned(id, auth);
        var current = prompts.findForUpdate(id).orElseThrow();
        if (request.expectedRevision() == null || request.expectedRevision() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected revision is required and must be nonnegative");
        if (!Objects.equals(current.getRevision(), request.expectedRevision()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instructions changed in another session. Reload the saved instructions before saving again.");
        var error = promptValidator.validate(request.prompt());
        if (error.isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", error.get().details()));
        current.change(request.prompt());
        prompts.flush();
        return promptResponse(current);
    }

    private AgentPromptResponse promptResponse(AgentPrompt p) {
        return new AgentPromptResponse(p.getAgentId(), p.getPrompt(), p.getRevision(), !p.getPrompt().isEmpty(), p.getUpdatedAt());
    }

    private AgentResponse response(ExamAgent a) {
        return response(a, prompts.findConfiguredIds(List.of(a.getId())).contains(a.getId()));
    }

    private AgentResponse response(ExamAgent a, boolean configured) {
        return new AgentResponse(a.getId(), a.getName(), a.getDescription(), a.getCreatedAt(), a.getUpdatedAt(), configured);
    }
}
