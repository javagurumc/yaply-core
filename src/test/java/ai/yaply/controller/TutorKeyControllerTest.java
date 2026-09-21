package ai.yaply.controller;

import ai.yaply.dto.TutorKeyStatus;
import ai.yaply.service.*;
import ai.yaply.testsupport.ClarityWebMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import java.time.Instant;

@ClarityWebMvcTest(TutorKeyController.class)
@ExtendWith(OutputCaptureExtension.class)
class TutorKeyControllerTest {
    @Autowired RestTestClient client;
    @MockitoBean TutorCredentialService service;
    private static final String PATH = "/api/tutor/api-key";
    @Test void rejectsAnonymousOperations() {
        client.get().uri(PATH).exchange().expectStatus().isUnauthorized();
        client.put().uri(PATH).contentType(MediaType.APPLICATION_JSON).body(Map.of("apiKey", "synthetic")).exchange().expectStatus().isUnauthorized();
        client.delete().uri(PATH).exchange().expectStatus().isUnauthorized();
        verifyNoInteractions(service);
    }
    @Test @WithMockUser(username = "tutor@example.com")
    void statusAndSaveReturnOnlyStatusAndUseAuthenticatedIdentity() {
        when(service.status(any())).thenReturn(new TutorKeyStatus(false, null));
        client.get().uri(PATH).exchange().expectStatus().isOk().expectHeader().valueEquals("Cache-Control", "no-store")
                .expectBody().jsonPath("$.configured").isEqualTo(false).jsonPath("$.updatedAt").isEmpty();
        when(service.save(any(), any())).thenReturn(new TutorKeyStatus(true, Instant.now()));
        client.put().uri(PATH).contentType(MediaType.APPLICATION_JSON).body(Map.of("apiKey", "synthetic", "ownerProfileId", "someone-else"))
                .exchange().expectStatus().isOk().expectHeader().valueEquals("Cache-Control", "no-store")
                .expectBody().jsonPath("$.apiKey").doesNotExist().jsonPath("$.ciphertext").doesNotExist();
        verify(service).save(argThat(auth -> auth.getName().equals("tutor@example.com")), eq("synthetic"));
        client.delete().uri(PATH).exchange().expectStatus().isNoContent().expectHeader().valueEquals("Cache-Control", "no-store");
    }
    @Test @WithMockUser
    void redactsMalformedRequestsAndInternalExceptions(CapturedOutput output) {
        String secret = "sentinel-credential-do-not-log";
        when(service.save(any(), any())).thenThrow(new RuntimeException(secret));
        client.put().uri(PATH).contentType(MediaType.APPLICATION_JSON).body(Map.of("apiKey", secret))
                .exchange().expectStatus().is5xxServerError().expectBody(String.class).value(body -> assertThat(body).doesNotContain(secret));
        client.put().uri(PATH).contentType(MediaType.APPLICATION_JSON).body("{\"apiKey\":\"" + secret)
                .exchange().expectStatus().isBadRequest().expectBody(String.class).value(body -> assertThat(body).doesNotContain(secret));
        assertThat(output.getAll()).doesNotContain(secret);
    }
    @Test @WithMockUser
    void setupFailureIsSanitized() {
        when(service.save(any(), any())).thenThrow(new CredentialFailure(CredentialFailure.Reason.SETUP));
        client.put().uri(PATH).contentType(MediaType.APPLICATION_JSON).body(Map.of("apiKey", "synthetic"))
                .exchange().expectStatus().isEqualTo(503).expectHeader().valueEquals("Cache-Control", "no-store");
    }
}
