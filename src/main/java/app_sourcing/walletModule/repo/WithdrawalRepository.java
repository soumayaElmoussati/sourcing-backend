package app_sourcing.walletModule.repo;

import app_sourcing.walletModule.entitie.SellerWallet;
import app_sourcing.walletModule.entitie.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Page<Withdrawal> findByWallet(SellerWallet wallet, Pageable pageable);
    Page<Withdrawal> findByWalletAndStatus(SellerWallet wallet, String status, Pageable pageable);
    Page<Withdrawal> findByStatus(String status, Pageable pageable);
    List<Withdrawal> findByWalletAndStatus(SellerWallet wallet, String status);
    Long countByWalletAndStatus(SellerWallet wallet, String status);

    Long countByStatus(String status);
}
