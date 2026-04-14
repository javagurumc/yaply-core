package ai.yaply.repo;

import ai.yaply.entity.KbDocument;
import ai.yaply.entity.KbScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for KbDocument entities.
 */
public interface KbDocumentRepository extends JpaRepository<KbDocument, UUID> {

    /**
     * Find documents by scope and user ID.
     */
    List<KbDocument> findByScopeAndUserId(KbScope scope, String userId);

    /**
     * Find documents by scope only (for GLOBAL and PROGRAM scopes).
     */
    List<KbDocument> findByScope(KbScope scope);

    /**
     * Find documents by source and scope.
     */
    List<KbDocument> findBySourceAndScope(String source, KbScope scope);

    /**
     * Find document by title, scope, and user (for upsert logic).
     */
    Optional<KbDocument> findByTitleAndScopeAndUserId(String title, KbScope scope, String userId);

    /**
     * Find document by title and scope for GLOBAL/PROGRAM.
     */
    Optional<KbDocument> findByTitleAndScopeAndUserIdIsNull(String title, KbScope scope);

    /**
     * Delete all documents by scope and user (for cleanup).
     */
    void deleteByScopeAndUserId(KbScope scope, String userId);
}
