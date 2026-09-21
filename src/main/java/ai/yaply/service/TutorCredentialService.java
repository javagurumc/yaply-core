package ai.yaply.service;

import ai.yaply.dto.TutorKeyStatus;
import ai.yaply.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutorCredentialService {
    private final TutorApiCredentialRepository credentials;
    private final ProfileRepository profiles;
    private final CurrentProfileService currentProfile;
    private final CredentialCrypto crypto;

    public TutorKeyStatus status(Authentication auth) {
        return credentials.findById(currentProfile.id(auth)).map(c -> new TutorKeyStatus(true, c.getUpdatedAt()))
                .orElse(new TutorKeyStatus(false, null));
    }
    @Transactional
    public TutorKeyStatus save(Authentication auth, String value) {
        UUID owner = currentProfile.id(auth);
        String key = value == null ? "" : value.strip();
        if (key.isEmpty() || key.length() > 4096 || key.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.isISOControl(c)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a nonempty API key of at most 4096 characters without internal whitespace or control characters.");
        lock(owner);
        var credential = crypto.encrypt(owner, key, Instant.now());
        credentials.saveAndFlush(credential);
        return new TutorKeyStatus(true, credential.getUpdatedAt());
    }
    @Transactional
    public void remove(Authentication auth) {
        UUID owner = currentProfile.id(auth);
        lock(owner);
        credentials.deleteById(owner);
    }
    // Server-only: callers must derive this ID from an authorized agent, never client input.
    public String resolve(UUID trustedOwnerId) {
        return crypto.decrypt(credentials.findById(trustedOwnerId)
                .orElseThrow(() -> new CredentialFailure(CredentialFailure.Reason.MISSING)));
    }
    @Transactional
    public boolean rotate(UUID owner) {
        lock(owner);
        var credential = credentials.findById(owner);
        if (credential.isEmpty() || credential.get().getEncryptionKeyId().equals(crypto.activeId())) return false;
        var existing = credential.get();
        credentials.saveAndFlush(crypto.encrypt(owner, crypto.decrypt(existing), existing.getUpdatedAt()));
        return true;
    }
    private void lock(UUID owner) {
        profiles.findLockedById(owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
