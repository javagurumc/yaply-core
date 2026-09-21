package ai.yaply.service;

import ai.yaply.entity.TutorApiCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Component
public class CredentialCrypto {
    private final String activeId;
    private final Map<String, SecretKeySpec> keys = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public CredentialCrypto(@Value("${TUTOR_CREDENTIAL_ACTIVE_KEY_ID:}") String activeId,
                            @Value("${TUTOR_CREDENTIAL_KEYS_JSON:}") String json) {
        this.activeId = activeId;
        if (activeId.isBlank() && json.isBlank()) return;
        try {
            var tree = new ObjectMapper().readTree(json);
            if (!tree.isObject() || !activeId.matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException();
            for (var entry : tree.properties()) {
                if (!entry.getKey().matches("[A-Za-z0-9_-]{1,100}") || !entry.getValue().isString()) throw new IllegalArgumentException();
                byte[] bytes = Base64.getDecoder().decode(entry.getValue().asString());
                if (bytes.length != 32) throw new IllegalArgumentException();
                keys.put(entry.getKey(), new SecretKeySpec(bytes, "AES"));
                Arrays.fill(bytes, (byte) 0);
            }
            if (!keys.containsKey(activeId)) throw new IllegalArgumentException();
        } catch (Exception ignored) {
            throw new IllegalStateException("Invalid tutor credential encryption configuration; check key IDs and Base64-encoded 32-byte keys.");
        }
    }

    public String activeId() { requireSetup(); return activeId; }
    public void requireSetup() {
        if (keys.isEmpty()) throw new CredentialFailure(CredentialFailure.Reason.SETUP);
    }
    private byte[] aad(UUID owner) {
        return ("yaply:tutor-api-key:v1:" + owner).getBytes(StandardCharsets.UTF_8);
    }
    public TutorApiCredential encrypt(UUID owner, String plaintext, Instant changedAt) {
        requireSetup();
        byte[] raw = plaintext.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeId), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(owner));
            return new TutorApiCredential(owner, cipher.doFinal(raw), nonce, activeId, changedAt);
        } catch (Exception ignored) {
            throw new CredentialFailure(CredentialFailure.Reason.SETUP);
        } finally { Arrays.fill(raw, (byte) 0); }
    }
    public String decrypt(TutorApiCredential credential) {
        requireSetup();
        byte[] raw = null;
        try {
            if (credential.getFormatVersion() != 1 || !keys.containsKey(credential.getEncryptionKeyId())) throw new IllegalArgumentException();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keys.get(credential.getEncryptionKeyId()), new GCMParameterSpec(128, credential.getNonce()));
            cipher.updateAAD(aad(credential.getOwnerProfileId()));
            raw = cipher.doFinal(credential.getCiphertext());
            return new String(raw, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            throw new CredentialFailure(CredentialFailure.Reason.UNREADABLE);
        } finally { if (raw != null) Arrays.fill(raw, (byte) 0); }
    }
}
