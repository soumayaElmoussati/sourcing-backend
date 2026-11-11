package app_sourcing.walletModule.repo;

import app_sourcing.walletModule.entitie.Fee;
import app_sourcing.walletModule.entitie.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    Optional<Fee> findByType(FeeType type);
    boolean existsByType(FeeType type);
}
