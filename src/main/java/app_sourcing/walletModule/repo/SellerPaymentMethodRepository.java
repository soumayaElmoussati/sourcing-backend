package app_sourcing.walletModule.repo;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.entitie.SellerPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerPaymentMethodRepository extends JpaRepository<SellerPaymentMethod, Long> {
    List<SellerPaymentMethod> findBySellerOrderByCreatedAtDesc(Seller seller);
    Optional<SellerPaymentMethod> findByIdAndSeller(Long id, Seller seller);
}
