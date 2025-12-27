package ai.claritywalk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(nullable = false)
    private String status; // STARTED, ENDED

    @Lob
    @Column(name = "session_config_json")
    private String sessionConfigJson;

    @Lob
    @Column(name = "final_summary_json")
    private String finalSummaryJson;

    public static Conversation started(UUID id, String userId, String sessionConfigJson) {
        Conversation c = new Conversation();
        c.id = id;
        c.userId = userId;
        c.startedAt = Instant.now();
        c.status = "STARTED";
        c.sessionConfigJson = sessionConfigJson;
        return c;
    }

    public void end(String finalSummaryJson) {
        this.status = "ENDED";
        this.endedAt = Instant.now();
        this.finalSummaryJson = finalSummaryJson;
    }

}
