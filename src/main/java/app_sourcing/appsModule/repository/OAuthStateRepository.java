// ─── FILE 2: OAuthStateRepository.java ───────────────────────────────────────
package app_sourcing.appsModule.repository;

import app_sourcing.appsModule.entitie.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OAuthStateRepository extends JpaRepository<OAuthState, String> {

    Optional<OAuthState> findByState(String state);

    @Modifying
    @Transactional
    @Query("DELETE FROM OAuthState s WHERE s.expiresAt < :now")
    void deleteExpiredStates(LocalDateTime now);
}