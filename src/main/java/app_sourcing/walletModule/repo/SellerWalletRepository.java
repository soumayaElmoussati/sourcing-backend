package app_sourcing.walletModule.repo;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.entitie.SellerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerWalletRepository extends JpaRepository<SellerWallet, Long> {
    Optional<SellerWallet> findBySellerId(Long sellerId);
    boolean existsBySellerId(Long sellerId);
    Optional<SellerWallet> findBySeller(Seller seller);
}