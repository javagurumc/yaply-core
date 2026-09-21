package ai.yaply.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_api_credential")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TutorApiCredential {
    @Id private UUID ownerProfileId;
    @Column(nullable = false) private byte[] ciphertext;
    @Column(nullable = false) private byte[] nonce;
    @Column(nullable = false, length = 100) private String encryptionKeyId;
    @Column(nullable = false) private int formatVersion;
    @Column(nullable = false) private Instant updatedAt;

    public TutorApiCredential(UUID owner, byte[] ciphertext, byte[] nonce, String keyId, Instant updatedAt) {
        this.ownerProfileId = owner;
        this.ciphertext = ciphertext;
        this.nonce = nonce;
        this.encryptionKeyId = keyId;
        this.formatVersion = 1;
        this.updatedAt = updatedAt;
    }
}
