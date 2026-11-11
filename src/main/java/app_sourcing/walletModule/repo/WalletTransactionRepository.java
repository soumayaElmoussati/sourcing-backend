package app_sourcing.walletModule.repo;

import app_sourcing.walletModule.entitie.SellerWallet;
import app_sourcing.walletModule.entitie.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletOrderByCreatedAtDesc(SellerWallet wallet, Pageable pageable);
    Page<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
