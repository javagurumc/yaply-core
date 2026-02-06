package ai.claritywalk.service;

import ai.claritywalk.dto.TranscriptBatchRequest;
import ai.claritywalk.entity.Conversation;
import ai.claritywalk.entity.ConversationEvent;
import ai.claritywalk.entity.KbScope;
import ai.claritywalk.entity.Profile;
import ai.claritywalk.repo.ConversationEventRepository;
import ai.claritywalk.repo.ConversationRepository;
import ai.claritywalk.repo.ProfileRepository;
import ai.claritywalk.util.JsonUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationEventRepository eventRepo;
    private final SummarizationService summarizationService;
    private final KbIngestService kbIngestService;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private final ProfileRepository profileRepository;

    @Transactional
    public UUID createConversation(String userId) {
        UUID id = UUID.randomUUID();

        // Fetch user profile and generate customized prompt
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));

        // Parse profile responses from JSON
        Map<String, String> responsesMap = objectMapper.readValue(
                profile.getResponses(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        // Generate customized system prompt
        String systemPrompt = promptService.generateSystemPrompt(responsesMap);

        // Create session config with the customized prompt
        Map<String, String> sessionConfig = new LinkedHashMap<>();
        sessionConfig.put("systemPrompt", systemPrompt);
        String sessionConfigJson = objectMapper.writeValueAsString(sessionConfig);

        conversationRepo.save(Conversation.started(id, userId, sessionConfigJson));
        log.info("Created conversation {} with customized prompt for user {}", id, userId);

        return id;
    }

    @Transactional
    public void appendEvents(TranscriptBatchRequest req) {
        for (var item : req.items()) {
            var event = ConversationEvent.of(
                    req.conversationId(),
                    item.ts(),
                    item.role(),
                    item.type(),
                    objectMapper.writeValueAsString(item.content()));
            log.info(event.toString());
            eventRepo.save(event);
        }
    }

    @Transactional
    public Map<String, Object> endAndSummarize(UUID conversationId, String userId) {
        Conversation convo = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        var events = eventRepo.findByConversationIdOrderByTsAsc(conversationId);
        Map<String, Object> summary = summarizationService.summarize(convo, events);

        convo.end(JsonUtil.toJson(summary));
        conversationRepo.save(convo);

        // Persist user memory to KB
        persistUserMemory(userId, summary, conversationId);

        return summary;
    }

    /**
     * Persist conversation summary as user memory in the knowledge base.
     */
    private void persistUserMemory(String userId, Map<String, Object> summary, UUID conversationId) {
        try {
            String memoryContent = buildMemoryContent(summary);
            List<String> tags = extractTags(summary);

            String title = "Walk on " + java.time.LocalDate.now().toString();

            kbIngestService.ingestDocument(
                    KbScope.USER,
                    userId,
                    "walk_summary",
                    title,
                    memoryContent,
                    tags,
                    1);

            log.info("Persisted user memory for conversation {} to KB", conversationId);

        } catch (Exception e) {
            log.error("Failed to persist user memory for conversation {}", conversationId, e);
            // Don't throw - memory persistence is not critical to conversation end
        }
    }

    /**
     * Build memory content from summary (summary + takeaways).
     */
    private String buildMemoryContent(Map<String, Object> summary) {
        StringBuilder content = new StringBuilder();

        // Add summary
        if (summary.containsKey("summary")) {
            content.append("Summary: ").append(summary.get("summary")).append("\n\n");
        }

        // Add takeaways
        if (summary.containsKey("takeaways") && summary.get("takeaways") instanceof List) {
            List<Map> takeaways = (List<Map>) summary.get("takeaways");
            content.append("Key Takeaways:\n");
            for (Map t : takeaways) {
                content.append("- ").append(t.get("title"))
                        .append(": ").append(t.get("detail")).append("\n");
            }
        }

        return content.toString();
    }

    /**
     * Extract tags from summary.
     */
    private List<String> extractTags(Map<String, Object> summary) {
        List<String> tags = new ArrayList<>();
        tags.add("walk");
        tags.add("summary");

        // Add tags from takeaways
        if (summary.containsKey("takeaways") && summary.get("takeaways") instanceof List) {
            List<Map> takeaways = (List<Map>) summary.get("takeaways");
            for (Map t : takeaways) {
                if (t.containsKey("tags") && t.get("tags") instanceof List) {
                    tags.addAll((List<String>) t.get("tags"));
                }
            }
        }

        return tags;
    }

    public Map<String, Object> getConversationView(UUID conversationId) {
        Conversation convo = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        var events = eventRepo.findByConversationIdOrderByTsAsc(conversationId);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("conversationId", convo.getId());
        view.put("status", convo.getStatus());
        view.put("startedAt", convo.getStartedAt());
        view.put("endedAt", convo.getEndedAt());
        view.put("finalSummary", convo.getFinalSummaryJson());
        view.put("events", events.stream().map(e -> Map.of(
                "ts", e.getTs(),
                "role", e.getRole(),
                "type", e.getType(),
                "content", e.getContent())).toList());
        return view;
    }
}
