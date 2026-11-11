package app_sourcing.walletModule.repo;

import app_sourcing.walletModule.entitie.Deposit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
    List<Deposit> findByWalletSellerIdOrderByCreatedAtDesc(Long sellerId);

    Page<Deposit> findAll(Pageable pageable);
}