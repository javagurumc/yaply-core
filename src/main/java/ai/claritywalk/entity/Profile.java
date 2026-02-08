package ai.claritywalk.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responses", nullable = false, columnDefinition = "jsonb")
    private String responses;

    @Setter
    @Column(name = "password_hash")
    private String passwordHash;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.EMAIL;

    @Setter
    @Column(name = "provider_user_id")
    private String providerUserId;

    @Setter
    @Column(name = "provider_email")
    private String providerEmail;

}
