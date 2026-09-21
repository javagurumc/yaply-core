package ai.yaply.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_agent")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ExamAgent {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID ownerProfileId;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 2000) private String description;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    public ExamAgent(UUID ownerProfileId, String name, String description) {
        this.id = UUID.randomUUID();
        this.ownerProfileId = ownerProfileId;
        this.createdAt = Instant.now();
        updateDetails(name, description);
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
    }
}
