package ai.claritywalk.repo;

import ai.claritywalk.entity.ConversationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationEventRepository extends JpaRepository<ConversationEvent, UUID> {

    List<ConversationEvent> findByConversationIdOrderByTsAsc(UUID conversationId);

}
