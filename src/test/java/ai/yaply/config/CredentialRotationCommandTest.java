package ai.yaply.config;

import ai.yaply.repo.TutorApiCredentialRepository;
import ai.yaply.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class CredentialRotationCommandTest {
    @Test void reportsOnlyCountsClosesAndSignalsPartialFailure(CapturedOutput output) {
        var repo = mock(TutorApiCredentialRepository.class);
        var service = mock(TutorCredentialService.class);
        var crypto = mock(CredentialCrypto.class);
        var context = mock(ConfigurableApplicationContext.class);
        var first = UUID.randomUUID(); var second = UUID.randomUUID(); var third = UUID.randomUUID();
        when(repo.findOwnerIds()).thenReturn(List.of(first, second, third));
        when(service.rotate(first)).thenReturn(true);
        when(service.rotate(second)).thenThrow(new RuntimeException("sentinel-secret-never-print"));
        when(service.rotate(third)).thenReturn(false);
        var command = new CredentialRotationCommand(repo, service, crypto, context);
        assertThatThrownBy(() -> command.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class).hasNoCause();
        verify(service).rotate(third);
        verify(context).close();
        assertThat(output.getAll()).contains("rotated=1, failed=1").doesNotContain("sentinel-secret-never-print");
    }
}
