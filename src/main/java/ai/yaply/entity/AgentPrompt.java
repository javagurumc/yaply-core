package ai.yaply.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_prompt")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AgentPrompt {
    @Id private UUID agentId;
    @Column(nullable = false, columnDefinition = "TEXT") private String prompt;
    @Version @Column(nullable = false) private Long revision;
    @Column(nullable = false) private Instant updatedAt;

    public AgentPrompt(UUID agentId) {
        this.agentId = agentId;
        this.prompt = "";
        this.updatedAt = Instant.now();
    }

    public void change(String prompt) {
        if (!this.prompt.equals(prompt)) {
            this.prompt = prompt;
            this.updatedAt = Instant.now();
        }
    }
}
