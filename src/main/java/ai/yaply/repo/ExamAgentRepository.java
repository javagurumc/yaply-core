package ai.yaply.repo;
import ai.yaply.entity.ExamAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExamAgentRepository extends JpaRepository<ExamAgent, UUID> {
    List<ExamAgent> findByOwnerProfileIdOrderByCreatedAtDescIdAsc(UUID ownerProfileId);
    Optional<ExamAgent> findByIdAndOwnerProfileId(UUID id, UUID ownerProfileId);
}
