package ai.claritywalk.service;

import ai.claritywalk.dto.TranscriptBatchRequest;
import ai.claritywalk.entity.Conversation;
import ai.claritywalk.entity.ConversationEvent;
import ai.claritywalk.repo.ConversationEventRepository;
import ai.claritywalk.repo.ConversationRepository;
import ai.claritywalk.util.JsonUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationEventRepository eventRepo;
    private final SummarizationService summarizationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID createConversation(String userId) {
        UUID id = UUID.randomUUID();
        conversationRepo.save(Conversation.started(id, userId, "{}"));
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
                    objectMapper.writeValueAsString(item.content())
            );
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
        return summary;
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
                "content", e.getContent()
        )).toList());
        return view;
    }
}
