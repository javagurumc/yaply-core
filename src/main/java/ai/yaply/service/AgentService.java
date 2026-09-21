package ai.yaply.service;

import ai.yaply.dto.*;
import ai.yaply.entity.ExamAgent;
import ai.yaply.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
    private final ProfileRepository profiles;

    private UUID owner(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return profiles.findByEmail(auth.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
    }

    public List<AgentResponse> list(Authentication auth) {
        return agents.findByOwnerProfileIdOrderByCreatedAtDescIdAsc(owner(auth)).stream().map(this::response).toList();
    }

    public AgentResponse get(UUID id, Authentication auth) { return response(owned(id, auth)); }

    @Transactional
    public AgentResponse create(CreateAgentRequest request, Authentication auth) {
        UUID ownerId = owner(auth);
        validate(request.name(), request.description());
        return response(agents.save(new ExamAgent(ownerId, request.name(), request.description())));
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

    private AgentResponse response(ExamAgent a) {
        return new AgentResponse(a.getId(), a.getName(), a.getDescription(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
