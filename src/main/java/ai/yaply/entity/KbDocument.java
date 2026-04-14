package ai.yaply.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Knowledge Base Document entity.
 * Stores documents in three scopes: GLOBAL (shared), PROGRAM (versioned), USER
 * (personal).
 */
@Entity
@Table(name = "kb_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KbDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KbScope scope;

    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT[]")
    private String[] tags;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Factory method for creating a new document.
     */
    public static KbDocument create(KbScope scope, String userId, String source,
            String title, String content, String[] tags, int version) {
        KbDocument doc = new KbDocument();
        doc.scope = scope;
        doc.userId = userId;
        doc.source = source;
        doc.title = title;
        doc.content = content;
        doc.tags = tags;
        doc.version = version;
        doc.createdAt = Instant.now();
        doc.updatedAt = Instant.now();
        return doc;
    }
}
