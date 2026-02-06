package ai.claritywalk.service;

import ai.claritywalk.config.KbConfig;
import ai.claritywalk.entity.Conversation;
import ai.claritywalk.entity.ConversationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummarizationService {

        private final KbConfig kbConfig;
        private final ObjectMapper objectMapper;
        private final RestTemplate restTemplate = new RestTemplate();

        @Value("${claritywalk.openai.apiKey}")
        private String openaiApiKey;

        private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

        public Map<String, Object> summarize(Conversation convo, List<ConversationEvent> events) {
                if (events.isEmpty()) {
                        return Map.of("summary", "No conversation events to summarize.");
                }

                // Format conversation for the prompt
                StringBuilder transcript = new StringBuilder();
                for (ConversationEvent event : events) {
                        // Only include user and assistant messages for summary
                        if ("text".equals(event.getType()) || "audio_transcript".equals(event.getType())) {
                                transcript.append(event.getRole().toUpperCase())
                                                .append(": ")
                                                .append(event.getContent())
                                                .append("\n");
                        }
                }

                if (transcript.isEmpty()) {
                        return Map.of("summary", "No dialogue content found.");
                }

                String systemPrompt = """
                                You are an expert conversation summarizer for a coaching AI.
                                Analyze the following conversation transcript.

                                Produce a JSON output with the following structure:
                                {
                                   "summary": "Concise paragraph summarizing the session (3-5 sentences).",
                                   "takeaways": [
                                      {
                                        "title": "Short title (e.g. 'Goal set')",
                                        "detail": "Description of the insight or action item",
                                        "tags": ["tag1", "tag2"]
                                      }
                                   ]
                                }

                                Focus on user goals, key insights, and action items.
                                Exclude technical details or small talk unless relevant to the user's progress.
                                """;

                try {
                        Map<String, Object> requestBody = Map.of(
                                        "model", kbConfig.getSummarization().getModel(),
                                        "messages", List.of(
                                                        Map.of("role", "system", "content", systemPrompt),
                                                        Map.of("role", "user", "content", transcript.toString())),
                                        "response_format", Map.of("type", "json_object"));

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(openaiApiKey);

                        HttpEntity<String> request = new HttpEntity<>(
                                        objectMapper.writeValueAsString(requestBody),
                                        headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        CHAT_COMPLETIONS_URL,
                                        HttpMethod.POST,
                                        request,
                                        String.class);

                        if (response.getStatusCode() != HttpStatus.OK) {
                                throw new RuntimeException("OpenAI API error: " + response.getStatusCode());
                        }

                        JsonNode root = objectMapper.readTree(response.getBody());
                        String content = root.path("choices").path(0).path("message").path("content").asText();

                        // Parse the content string as JSON
                        return objectMapper.readValue(content, new TypeReference<>() {});

                } catch (Exception e) {
                        log.warn("Failed to generate summary via LLM: {}", e.getMessage());

                        // Rule-based fallback
                        List<String> userLines = events.stream()
                                        .filter(evt -> "user".equals(evt.getRole()) && "text".equals(evt.getType()))
                                        .map(ConversationEvent::getContent)
                                        .toList();

                        String shortSummary = userLines.isEmpty()
                                        ? "No user text captured."
                                        : "User discussed: " + String.join(" | ",
                                                        userLines.subList(0, Math.min(3, userLines.size())));

                        return Map.of(
                                        "summary", shortSummary,
                                        "takeaways", List.of(
                                                        Map.of("title", "Session Note",
                                                                        "detail",
                                                                        "Automated summary unavailable due to high load. Review transcript for details.",
                                                                        "tags", List.of("fallback"))));
                }
        }
}
