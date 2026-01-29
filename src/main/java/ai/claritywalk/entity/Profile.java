package ai.claritywalk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
@Getter
@Entity
@Table(name = "profile")
public class Profile {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "email", nullable = false)
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responses", nullable = false, columnDefinition = "jsonb")
    private String responses;

}
