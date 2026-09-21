package ai.yaply.repo;

import ai.yaply.AbstractIntegrationTest;
import ai.yaply.entity.Profile;
import ai.yaply.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.time.Instant;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@TestPropertySource(properties = {
    "TUTOR_CREDENTIAL_ACTIVE_KEY_ID=new",
    "TUTOR_CREDENTIAL_KEYS_JSON={\"old\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"new\":\"AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=\"}"
})
class TutorCredentialIntegrationTest extends AbstractIntegrationTest {
    @Autowired TutorCredentialService service;
    @Autowired TutorApiCredentialRepository credentials;
    @Autowired ProfileRepository profiles;
    @Autowired CurrentProfileService current;
    @Autowired PlatformTransactionManager transactions;

    private Profile profile() {
        var id = UUID.randomUUID();
        return profiles.saveAndFlush(Profile.builder().id(id).userId(id.toString()).email(id + "@example.com").responses("{}").build());
    }
    private Authentication auth(Profile p) {
        return new UsernamePasswordAuthenticationToken(p.getEmail(), "", List.of());
    }
    private void oldCredential(Profile p) {
        var old = new CredentialCrypto("old", "{\"old\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"}");
        credentials.saveAndFlush(old.encrypt(p.getId(), "synthetic-old-key", Instant.now()));
    }
    @Test void isolatesTutorsAndReplacesAndRemovesWithoutPlaintextStorage() {
        var a = profile(); var b = profile();
        assertThat(service.status(auth(a)).configured()).isFalse();
        service.save(auth(a), "  synthetic-key-a  ");
        service.save(auth(b), "synthetic-key-b");
        assertThat(service.resolve(a.getId())).isEqualTo("synthetic-key-a");
        assertThat(service.resolve(b.getId())).isEqualTo("synthetic-key-b");
        assertThat(new String(credentials.findById(a.getId()).orElseThrow().getCiphertext(), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("synthetic-key-a");
        var prior = credentials.findById(a.getId()).orElseThrow().getCiphertext().clone();
        service.save(auth(a), "synthetic-key-a");
        assertThat(credentials.findById(a.getId()).orElseThrow().getCiphertext()).isNotEqualTo(prior);
        service.save(auth(a), "replacement-key");
        assertThat(service.resolve(a.getId())).isEqualTo("replacement-key");
        service.remove(auth(a)); service.remove(auth(a));
        assertThat(service.status(auth(a)).configured()).isFalse();
        assertThatThrownBy(() -> service.resolve(a.getId())).isInstanceOf(CredentialFailure.class);
        assertThat(service.resolve(b.getId())).isEqualTo("synthetic-key-b");
    }
    @Test void validationAndUnavailableCryptoPreserveExistingCredential() {
        var p = profile(); service.save(auth(p), "original-key");
        for (String invalid : Arrays.asList(null, "", "  ", "has space", "has\nnewline", "bad\u0000control", "x".repeat(4097))) {
            assertThatThrownBy(() -> service.save(auth(p), invalid)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        var unavailable = new TutorCredentialService(credentials, profiles, current, new CredentialCrypto("", ""));
        assertThat(unavailable.status(auth(p)).configured()).isTrue();
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> unavailable.save(auth(p), "replacement"))).isInstanceOf(CredentialFailure.class);
        assertThat(service.resolve(p.getId())).isEqualTo("original-key");
        new TransactionTemplate(transactions).executeWithoutResult(status -> unavailable.remove(auth(p)));
        assertThat(service.status(auth(p)).configured()).isFalse();
    }
    @Test void rotationPreservesTimestampAndCanResumeAfterUnreadableRow() {
        var good = profile(); var broken = profile(); oldCredential(good); oldCredential(broken);
        var before = credentials.findById(good.getId()).orElseThrow();
        var corrupt = credentials.findById(broken.getId()).orElseThrow();
        corrupt.getCiphertext()[0] ^= 1; credentials.saveAndFlush(corrupt);
        assertThat(service.rotate(good.getId())).isTrue();
        assertThat(service.resolve(good.getId())).isEqualTo("synthetic-old-key");
        var after = credentials.findById(good.getId()).orElseThrow();
        assertThat(after.getEncryptionKeyId()).isEqualTo("new");
        assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
        assertThat(service.rotate(good.getId())).isFalse();
        assertThatThrownBy(() -> service.rotate(broken.getId())).isInstanceOf(CredentialFailure.class);
        assertThat(credentials.findById(broken.getId()).orElseThrow().getEncryptionKeyId()).isEqualTo("old");
        service.save(auth(broken), "recovered-key");
        assertThat(service.rotate(broken.getId())).isFalse();
        assertThat(service.resolve(broken.getId())).isEqualTo("recovered-key");
    }
    @Test void concurrentFirstSavesCreateOnlyOneRow() throws Exception {
        var p = profile(); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); return service.save(auth(p), "first-key"); });
            var b = executor.submit(() -> { start.await(); return service.save(auth(p), "second-key"); });
            start.countDown();
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
            assertThat(service.resolve(p.getId())).isIn("first-key", "second-key");
            assertThat(credentials.findOwnerIds()).containsOnlyOnce(p.getId());
        }
    }
    @Test void queuedRotationCannotRestoreRemovedOrReplacedKey() throws Exception {
        for (boolean remove : List.of(true, false)) {
            var p = profile(); oldCredential(p);
            try (var executor = Executors.newSingleThreadExecutor()) {
                var entered = new CountDownLatch(1);
                var future = new java.util.concurrent.atomic.AtomicReference<Future<Boolean>>();
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    profiles.findLockedById(p.getId()).orElseThrow();
                    future.set(executor.submit(() -> { entered.countDown(); return service.rotate(p.getId()); }));
                    try { assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                    if (remove) service.remove(auth(p)); else service.save(auth(p), "newest-key");
                });
                assertThat(future.get().get(15, TimeUnit.SECONDS)).isFalse();
                if (remove) assertThat(service.status(auth(p)).configured()).isFalse();
                else assertThat(service.resolve(p.getId())).isEqualTo("newest-key");
            }
        }
    }
}
