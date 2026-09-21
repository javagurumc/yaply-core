package ai.yaply.repo;

import ai.yaply.entity.AuthProvider;
import ai.yaply.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Profile p where p.id = :id")
    Optional<Profile> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByUserId(String userId);

    Optional<Profile> findByAuthProviderAndProviderUserId(AuthProvider authProvider, String providerUserId);

    Optional<Profile> findByEmailAndAuthProvider(String email, AuthProvider authProvider);
}
