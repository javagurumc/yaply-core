package ai.claritywalk.service;

import ai.claritywalk.dto.CreateRealtimeSessionRequest;
import ai.claritywalk.dto.CreateRealtimeSessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
public class OpenAIRealtimeService {

    @Value("${claritywalk.openai.apiKey}")
    private String apiKey;

    @Value("${claritywalk.openai.realtimeSessionsUrl}")
    private String sessionsUrl;

    @Value("${claritywalk.openai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public CreateRealtimeSessionResponse createEphemeralSession(CreateRealtimeSessionRequest req, String userId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not set");
        }

        // Endpoint must be /v1/realtime/client_secrets
        String url = "https://api.openai.com/v1/realtime/client_secrets";

        // Body shape must be { expires_after?, session: {...} }
        Map<String, Object> body = Map.of(
                "session", Map.of(
                        "type", "realtime",
                        "model", "gpt-realtime",
                        "instructions", "You are Ricky Jarvis a coach for confused entrepreneurs. Ask one question at a time, reflect and keep responses short.",
                        "audio", Map.of(
                                "output", Map.of("voice", req.voice())
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Failed to create client secret: " + resp.getStatusCode());
        }

        Map<String, Object> json = resp.getBody();

        // Response differs by endpoint; handle both common shapes:
        // 1) { "client_secret": { "value": "...", "expires_at": ... }, ... }
        // 2) { "value": "...", "expires_at": ... , ... }
        String value = null;
        Instant expiresAt = Instant.now().plusSeconds(60);

        Object clientSecretObj = json.get("client_secret");
        if (clientSecretObj instanceof Map<?, ?> m) {
            Object v = m.get("value");
            if (v instanceof String s) value = s;
            Object ex = m.get("expires_at");
            if (ex instanceof Number n) expiresAt = Instant.ofEpochSecond(n.longValue());
        }
        if (value == null && json.get("value") instanceof String s) {
            value = s;
        }
        if (json.get("expires_at") instanceof Number n) {
            expiresAt = Instant.ofEpochSecond(n.longValue());
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("OpenAI did not return an ephemeral token value");
        }

        return new CreateRealtimeSessionResponse(req.conversationId(), value, expiresAt, "gpt-realtime");
    }

}
