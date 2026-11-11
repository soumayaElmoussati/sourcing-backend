package app_sourcing.productModule.repo;

import app_sourcing.productModule.entitie.ProductRejection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRejectionRepository
        extends JpaRepository<ProductRejection, String> {
}
