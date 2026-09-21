package ai.yaply.repo;
import ai.yaply.entity.TutorApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;
public interface TutorApiCredentialRepository extends JpaRepository<TutorApiCredential, UUID> {
    @Query("select c.ownerProfileId from TutorApiCredential c")
    List<UUID> findOwnerIds();
}
