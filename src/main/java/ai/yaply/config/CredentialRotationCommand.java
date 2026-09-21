package ai.yaply.config;

import ai.yaply.repo.TutorApiCredentialRepository;
import ai.yaply.service.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.credentials.rotate", havingValue = "true")
@RequiredArgsConstructor
public class CredentialRotationCommand implements ApplicationRunner {
    private final TutorApiCredentialRepository repository;
    private final TutorCredentialService service;
    private final CredentialCrypto crypto;
    private final ConfigurableApplicationContext context;
    @Override public void run(ApplicationArguments args) {
        try {
            crypto.requireSetup();
            int rotated = 0, failed = 0;
            for (var owner : repository.findOwnerIds()) {
                try { if (service.rotate(owner)) rotated++; }
                catch (Exception ignored) { failed++; }
            }
            System.out.println("Credential rotation: rotated=" + rotated + ", failed=" + failed);
            if (failed > 0) throw new IllegalStateException("Credential rotation incomplete; retain old encryption keys and retry.");
        } finally { context.close(); }
    }
}
