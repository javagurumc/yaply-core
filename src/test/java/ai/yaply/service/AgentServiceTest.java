package ai.yaply.service;

import ai.yaply.dto.*;
import ai.yaply.entity.*;
import ai.yaply.repo.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AgentServiceTest {
    private final ExamAgentRepository agents = mock(ExamAgentRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final AgentService service = new AgentService(agents, profiles);
    private final UUID owner = UUID.randomUUID();
    private final Authentication auth = new UsernamePasswordAuthenticationToken("teacher@example.com", "", List.of());

    @BeforeEach void setup() {
        when(profiles.findByEmail(auth.getName())).thenReturn(Optional.of(Profile.builder().id(owner).build()));
        when(agents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @Test void createsMultipleAgentsForAuthenticatedOwnerAndNormalizesInput() {
        var first = service.create(new CreateAgentRequest("  Biology  ", null), auth);
        var second = service.create(new CreateAgentRequest("Biology", " Cells "), auth);
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.name()).isEqualTo("Biology");
        assertThat(first.description()).isEmpty();
        verify(agents, times(2)).save(argThat(a -> a.getOwnerProfileId().equals(owner)));
    }
    @Test void listsOnlyOwnedAgents() {
        when(agents.findByOwnerProfileIdOrderByCreatedAtDescIdAsc(owner)).thenReturn(List.of(new ExamAgent(owner, "Math", "")));
        assertThat(service.list(auth)).extracting(AgentResponse::name).containsExactly("Math");
        verify(agents).findByOwnerProfileIdOrderByCreatedAtDescIdAsc(owner);
    }
    @Test void updatesPreserveIdentityOwnershipAndCreationTime() {
        var agent = new ExamAgent(owner, "Old", "");
        var created = agent.getCreatedAt();
        when(agents.findByIdAndOwnerProfileId(agent.getId(), owner)).thenReturn(Optional.of(agent));
        var response = service.update(agent.getId(), new UpdateAgentRequest(" New ", " Desc "), auth);
        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Desc");
        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isAfterOrEqualTo(created);
        assertThat(agent.getOwnerProfileId()).isEqualTo(owner);
    }
    @Test void missingOrForeignAgentsCannotBeReadOrUpdated() {
        var foreignId = UUID.randomUUID();
        when(agents.findByIdAndOwnerProfileId(foreignId, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(foreignId, auth)).isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThatThrownBy(() -> service.update(foreignId, new UpdateAgentRequest("Attack", ""), auth)).isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(agents, never()).save(any());
    }
    @Test void rejectsMissingIdentityAndMissingProfile() {
        assertThatThrownBy(() -> service.list(null)).isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
        when(profiles.findByEmail(auth.getName())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(auth)).isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
        verifyNoInteractions(agents);
    }
    @Test void rejectsInvalidMetadataEvenOutsideController() {
        for (String name : Arrays.asList(null, "  ", "x".repeat(121))) {
            assertThatThrownBy(() -> service.create(new CreateAgentRequest(name, ""), auth)).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> service.create(new CreateAgentRequest("Math", "x".repeat(2001)), auth)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.create(new CreateAgentRequest("x".repeat(120), "x".repeat(2000)), auth).name()).hasSize(120);
    }
}
