package app_sourcing.productModule.repo;

import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.entitie.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByIsArchiveFalse();

    Page<Product> findByIsArchiveFalse(Pageable pageable);

    Optional<Product> findByIdAndIsArchiveFalse(String id);

    List<Product> findBySellerIdAndIsArchiveFalse(Long sellerId);

    Page<Product> findBySellerIdAndIsArchiveFalse(Long sellerId, Pageable pageable);

    // ✅ Ligne ajoutée pour OrderIngestionService
    Optional<Product> findByIdAndSellerIdAndIsArchiveFalse(String id, Long sellerId);

    // Comptage
    long countByIsArchiveFalse();
    long countByStatusAndIsArchiveFalse(ProductStatus status);
    long countByStatusAndSellerIdAndIsArchiveFalse(ProductStatus status, Long sellerId);
    long countBySellerIdAndIsArchiveFalse(Long sellerId);
}