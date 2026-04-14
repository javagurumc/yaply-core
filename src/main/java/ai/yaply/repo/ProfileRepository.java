package ai.yaply.repo;

import ai.yaply.entity.AuthProvider;
import ai.yaply.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByUserId(String userId);

    Optional<Profile> findByAuthProviderAndProviderUserId(AuthProvider authProvider, String providerUserId);

    Optional<Profile> findByEmailAndAuthProvider(String email, AuthProvider authProvider);
}
