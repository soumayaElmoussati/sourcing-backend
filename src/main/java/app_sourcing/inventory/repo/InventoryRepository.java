package app_sourcing.inventory.repo;

import app_sourcing.inventory.entitie.Inventory;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.sourcingModule.entitie.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Optional<Inventory> findByProductAndWarehouse(Product product, Country warehouse);
}
