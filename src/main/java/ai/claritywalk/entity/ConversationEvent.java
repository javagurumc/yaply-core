package ai.claritywalk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "conversation_event", indexes = {
        @Index(name="idx_conv_event_conv_ts", columnList = "conversationId, ts")
})
public class ConversationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Column(name = "role", nullable = false)
    private String role; // user|assistant|tool|system

    @Column(name = "type", nullable = false)
    private String type; // text|transcript|tool_output|...

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    public static ConversationEvent of(UUID conversationId, Instant ts, String role, String type, String content) {
        ConversationEvent e = new ConversationEvent();
        e.conversationId = conversationId;
        e.ts = ts;
        e.role = role;
        e.type = type;
        e.content = content;
        return e;
    }

}
