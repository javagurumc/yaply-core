package ai.yaply.service;

import ai.yaply.entity.TutorApiCredential;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CredentialCryptoTest {
    static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    static final String JSON = "{\"first\":\"" + KEY + "\"}";
    final CredentialCrypto crypto = new CredentialCrypto("first", JSON);
    final UUID owner = UUID.randomUUID();
    final String sentinel = "synthetic-sentinel-never-log";

    @Test void encryptsWithFreshNoncesAndAuthenticatedOwner() {
        var a = crypto.encrypt(owner, sentinel, Instant.now());
        var b = crypto.encrypt(owner, sentinel, Instant.now());
        assertThat(a.getNonce()).hasSize(12).isNotEqualTo(b.getNonce());
        assertThat(a.getCiphertext()).isNotEqualTo(b.getCiphertext());
        assertThat(new String(a.getCiphertext(), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain(sentinel);
        assertThat(crypto.decrypt(a)).isEqualTo(sentinel);
        var copied = new TutorApiCredential(UUID.randomUUID(), a.getCiphertext(), a.getNonce(), "first", a.getUpdatedAt());
        assertThatThrownBy(() -> crypto.decrypt(copied)).isInstanceOf(CredentialFailure.class).hasMessageNotContaining(sentinel).hasNoCause();
    }
    @Test void rejectsTamperingWrongKeysAndUnknownKeyIds() {
        var c = crypto.encrypt(owner, sentinel, Instant.now());
        byte[] corrupt = c.getCiphertext().clone(); corrupt[0] ^= 1;
        assertThatThrownBy(() -> crypto.decrypt(new TutorApiCredential(owner, corrupt, c.getNonce(), "first", c.getUpdatedAt()))).isInstanceOf(CredentialFailure.class);
        byte[] nonce = c.getNonce().clone(); nonce[0] ^= 1;
        assertThatThrownBy(() -> crypto.decrypt(new TutorApiCredential(owner, c.getCiphertext(), nonce, "first", c.getUpdatedAt()))).isInstanceOf(CredentialFailure.class);
        assertThatThrownBy(() -> crypto.decrypt(new TutorApiCredential(owner, c.getCiphertext(), c.getNonce(), "missing", c.getUpdatedAt()))).isInstanceOf(CredentialFailure.class);
        byte[] other = new byte[32]; Arrays.fill(other, (byte) 1);
        var wrong = new CredentialCrypto("first", "{\"first\":\"" + Base64.getEncoder().encodeToString(other) + "\"}");
        assertThatThrownBy(() -> wrong.decrypt(c)).isInstanceOf(CredentialFailure.class);
    }
    @Test void configurationFailsClosedWithoutLeakingInput() {
        var absent = new CredentialCrypto("", "");
        assertThatThrownBy(absent::requireSetup).isInstanceOf(CredentialFailure.class);
        for (String malformed : List.of(sentinel, "{}", "{\"first\":\"bad\"}", "{\"first\":42}")) {
            assertThatThrownBy(() -> new CredentialCrypto("first", malformed)).isInstanceOf(IllegalStateException.class)
                    .hasMessageNotContaining(sentinel).hasNoCause();
        }
        assertThatThrownBy(() -> new CredentialCrypto("", JSON)).isInstanceOf(IllegalStateException.class);
    }
    @Test void mixedKeyRingReadsOldKeyAndWritesActiveKey() {
        var old = crypto.encrypt(owner, sentinel, Instant.now());
        byte[] bytes = new byte[32]; Arrays.fill(bytes, (byte) 2);
        var rotated = new CredentialCrypto("second", "{\"first\":\"" + KEY + "\",\"second\":\"" + Base64.getEncoder().encodeToString(bytes) + "\"}");
        assertThat(rotated.decrypt(old)).isEqualTo(sentinel);
        var replacement = rotated.encrypt(owner, rotated.decrypt(old), old.getUpdatedAt());
        assertThat(replacement.getEncryptionKeyId()).isEqualTo("second");
        assertThat(rotated.decrypt(replacement)).isEqualTo(sentinel);
    }
}
