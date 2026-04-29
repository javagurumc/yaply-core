package ai.yaply.service;

import ai.yaply.dto.TranscriptBatchRequest;
import ai.yaply.entity.Conversation;
import ai.yaply.entity.ConversationEvent;
import ai.yaply.entity.KbScope;
import ai.yaply.entity.Profile;
import ai.yaply.repo.ConversationEventRepository;
import ai.yaply.repo.ConversationRepository;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.util.JsonUtil;
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
    private static final String ANONYMOUS_USER_ID = "anonymous";


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
        String resolvedUserId = userId == null || userId.isBlank() ? ANONYMOUS_USER_ID : userId;

        Map<String, String> responsesMap = Map.of();
        String customTutorPrompt = "";
        
        if (!ANONYMOUS_USER_ID.equals(resolvedUserId)) {
            Profile profile = profileRepository.findByUserId(resolvedUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + resolvedUserId));

            responsesMap = objectMapper.readValue(
                    profile.getResponses(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            
            customTutorPrompt = profile.getCustomTutorPrompt() != null ? profile.getCustomTutorPrompt() : "";
        }

        // Use custom prompt if available, otherwise generate from questionnaire responses
        String systemPrompt;
        if (customTutorPrompt != null && !customTutorPrompt.trim().isEmpty()) {
            systemPrompt = customTutorPrompt;
            log.info("Created conversation {} with custom tutor prompt for user {}", id, resolvedUserId);
        } else {
            systemPrompt = promptService.generateSystemPrompt(responsesMap);
            log.info("Created conversation {} with questionnaire-based prompt for user {}", id, resolvedUserId);
        }

        // Create session config with the selected prompt
        Map<String, String> sessionConfig = new LinkedHashMap<>();
        sessionConfig.put("systemPrompt", systemPrompt);
        String sessionConfigJson = objectMapper.writeValueAsString(sessionConfig);

        conversationRepo.save(Conversation.started(id, resolvedUserId, sessionConfigJson));

        return id;
    }

    public String getSystemPrompt(UUID conversationId, String userId) {
        Conversation convo = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (userId != null && !userId.isBlank() && !Objects.equals(convo.getUserId(), userId)) {
            throw new IllegalArgumentException("Conversation does not belong to user");
        }

        String sessionConfigJson = convo.getSessionConfigJson();
        if (sessionConfigJson == null || sessionConfigJson.isBlank()) {
            throw new IllegalStateException("Conversation session config is missing");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> sessionConfig = objectMapper.readValue(
                sessionConfigJson,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

        Object systemPrompt = sessionConfig.get("systemPrompt");
        if (systemPrompt instanceof String s && !s.isBlank()) {
            return s;
        }

        throw new IllegalStateException("Conversation system prompt is missing");
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
        if (userId != null && !userId.isBlank() && !ANONYMOUS_USER_ID.equals(userId)) {
            persistUserMemory(userId, summary, conversationId);
        }

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
