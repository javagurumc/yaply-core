package ai.claritywalk.service;

import ai.claritywalk.dto.CreateRealtimeSessionRequest;
import ai.claritywalk.dto.CreateRealtimeSessionResponse;
import ai.claritywalk.util.JsonUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
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

        // Minimal server-side session config; expand as needed:
        // - instructions
        // - voice
        // - modalities
        // - tools schema (you can also send tools from client via session.update)
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);

        // example config fields that many Realtime setups use:
        Map<String, Object> session = new HashMap<>();
        session.put("voice", req.voice());
        session.put("instructions", "You are Clarity Walk coach. Ask one question at a time. Keep responses short.");
        body.put("session", session);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toJson(body), headers);

        // Response shape depends on API; parse loosely into Map first
        ResponseEntity<Map> resp = restTemplate.exchange(sessionsUrl, HttpMethod.POST, entity, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Failed to create realtime session: " + resp.getStatusCode());
        }

        Map<String, Object> json = resp.getBody();

        // Expect something like: { client_secret: { value, expires_at }, ... }
        Map<String, Object> clientSecret = (Map<String, Object>) json.get("client_secret");
        String value = clientSecret != null ? (String) clientSecret.get("value") : null;

        Instant expiresAt = Instant.now().plusSeconds(60);
        Object expires = clientSecret != null ? clientSecret.get("expires_at") : null;
        if (expires instanceof Number n) {
            expiresAt = Instant.ofEpochSecond(n.longValue());
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("OpenAI did not return client_secret.value");
        }

        return new CreateRealtimeSessionResponse(req.conversationId(), value, expiresAt, model);
    }
}
