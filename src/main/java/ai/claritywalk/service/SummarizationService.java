package ai.claritywalk.service;

import ai.claritywalk.entity.Conversation;
import ai.claritywalk.entity.ConversationEvent;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SummarizationService {

    public Map<String, Object> summarize(Conversation convo, List<ConversationEvent> events) {
        // MVP stub: later replace with LLM call that returns strict JSON
        // You can pass: rolling summary + last N turns, etc.

        List<String> userLines = events.stream()
                .filter(e -> "user".equals(e.getRole()) && "text".equals(e.getType()))
                .map(ConversationEvent::getContent)
                .toList();

        String shortSummary = userLines.isEmpty()
                ? "No user text captured."
                : "User discussed: " + String.join(" | ", userLines.subList(0, Math.min(3, userLines.size())));

        return new LinkedHashMap<>(Map.of(
                "summary", shortSummary,
                "takeaways", List.of(
                        Map.of("title", "Main topic", "detail", "Captured from user messages", "category", "insight", "confidence", 0.6),
                        Map.of("title", "Next step", "detail", "Suggest a small walk action", "category", "action", "confidence", 0.5),
                        Map.of("title", "Support", "detail", "Maintain a calm pace", "category", "action", "confidence", 0.5)
                )
        ));
    }

}
