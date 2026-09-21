package ai.yaply.repo;

import ai.yaply.entity.AgentPrompt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AgentPromptRepository extends JpaRepository<AgentPrompt, UUID> {
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select p from AgentPrompt p where p.agentId = :id")
    Optional<AgentPrompt> findForUpdate(@Param("id") UUID id);

    @Query("select p.agentId from AgentPrompt p where p.agentId in :ids and p.prompt <> ''")
    Set<UUID> findConfiguredIds(@Param("ids") Collection<UUID> ids);
}
