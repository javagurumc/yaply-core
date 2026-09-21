package ai.yaply.repo;

import ai.yaply.AbstractIntegrationTest;
import ai.yaply.dto.*;
import ai.yaply.entity.*;
import ai.yaply.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ExamAgentIntegrationTest extends AbstractIntegrationTest {
    @Autowired ExamAgentRepository agents;
    @Autowired ProfileRepository profiles;
    @Autowired AgentService service;

    private Profile profile() {
        var id = UUID.randomUUID();
        return profiles.saveAndFlush(Profile.builder().id(id).userId(id.toString())
                .email(id + "@example.com").responses("{}").build());
    }

    @Test void persistsEditsAndIsolatesTwoTutors() {
        var a = profile(); var b = profile();
        var authA = new UsernamePasswordAuthenticationToken(a.getEmail(), "", List.of());
        var authB = new UsernamePasswordAuthenticationToken(b.getEmail(), "", List.of());
        var first = service.create(new CreateAgentRequest("Math", "Exam practice"), authA);
        var second = service.create(new CreateAgentRequest("Biology", ""), authA);
        service.create(new CreateAgentRequest("Physics", ""), authB);
        assertThat(service.list(authA)).extracting(AgentResponse::id).containsExactly(second.id(), first.id());
        assertThat(service.list(authB)).extracting(AgentResponse::name).containsExactly("Physics");
        assertThatThrownBy(() -> service.get(first.id(), authB)).isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThatThrownBy(() -> service.update(first.id(), new UpdateAgentRequest("Stolen", ""), authB)).isInstanceOf(ResponseStatusException.class);
        service.update(first.id(), new UpdateAgentRequest("Advanced math", "Updated"), authA);
        var persisted = agents.findById(first.id()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Advanced math");
        assertThat(persisted.getOwnerProfileId()).isEqualTo(a.getId());
        assertThat(persisted.getCreatedAt()).isEqualTo(service.get(first.id(), authA).createdAt());
    }

    @Test void databaseEnforcesOwnerAndNonblankName() {
        assertThatThrownBy(() -> agents.saveAndFlush(new ExamAgent(UUID.randomUUID(), "Math", "")))
                .isInstanceOf(DataIntegrityViolationException.class);
        var owner = profile();
        assertThatThrownBy(() -> agents.saveAndFlush(new ExamAgent(owner.getId(), "  ", "")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
