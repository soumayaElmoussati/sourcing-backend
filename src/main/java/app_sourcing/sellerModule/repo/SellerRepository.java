package app_sourcing.sellerModule.repo;


import app_sourcing.sellerModule.entitie.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    // Soft delete
    List<Seller> findAllByDeletedFalse();
    List<Seller> findAllByDeletedTrue();

}
