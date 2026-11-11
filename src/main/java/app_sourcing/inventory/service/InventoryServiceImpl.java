package app_sourcing.inventory.service;

import app_sourcing.inventory.entitie.Inventory;
import app_sourcing.inventory.exception.BusinessInventoryException;
import app_sourcing.inventory.repo.InventoryRepository;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.shareClasses.IdGenerator;
import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl {

    private final InventoryRepository inventoryRepository;
    
        // =========================
        // CREATE INVENTORY (Product activation)
        // =========================
        public void createInventory(Product product, int initialStock) {

            Country warehouse =
                    product.getFulfillmentMode() == FulfillmentMode.CENTRALIZED
                            ? Country.UAE
                            : product.getCountry();

            // 🔒 Prevent duplication
            inventoryRepository.findByProductAndWarehouse(product, warehouse)
                    .ifPresent(inv -> {
                        throw new BusinessInventoryException(
                                "Inventory already exists for this product"
                        );
                    });

            Inventory inventory = new Inventory();
            inventory.setId(IdGenerator.generateId("IN"));
            inventory.setProduct(product);
            inventory.setWarehouse(warehouse);

            inventory.setAvailable(initialStock);
            inventory.setReserved(0);
            inventory.setReturned(0);

            inventory.setLastUpdate(LocalDateTime.now());

            inventoryRepository.save(inventory);
        }


    // =========================
    // RESERVE (CONFIRMED)
    // =========================
    public void reserveStock(Product product, int quantity) {

        Inventory inventory = getInventory(product);

        if (inventory.getAvailable() < quantity) {
            throw new BusinessInventoryException("Insufficient available stock");
        }

        inventory.setAvailable(inventory.getAvailable() - quantity);
        inventory.setReserved(inventory.getReserved() + quantity);
        inventory.setLastUpdate(LocalDateTime.now());
    }

    // =========================
    // RETURNED
    // =========================
    public void moveReservedToReturned(Product product, int quantity) {

        Inventory inventory = getInventory(product);

        if (inventory.getReserved() < quantity) {
            throw new BusinessInventoryException("Invalid returned quantity");
        }

        inventory.setReserved(inventory.getReserved() - quantity);
        inventory.setReturned(inventory.getReturned() + quantity);

        inventory.setLastUpdate(LocalDateTime.now());
    }

    // =========================
    // ADMIN ACTION
    // =========================
    public void moveReturnedToAvailable(Product product, int quantity) {

        Inventory inventory = getInventory(product);

        if (inventory.getReturned() < quantity) {
            throw new BusinessInventoryException("Invalid returned stock");
        }

        inventory.setReturned(inventory.getReturned() - quantity);
        inventory.setAvailable(inventory.getAvailable() + quantity);
        inventory.setLastUpdate(LocalDateTime.now());
    }

    // =========================
    // INTERNAL
    // =========================
    public Inventory getInventory(Product product) {

        Country warehouse =
                product.getFulfillmentMode() == FulfillmentMode.CENTRALIZED
                        ? Country.UAE
                        : product.getCountry();

        return inventoryRepository
                .findByProductAndWarehouse(product, warehouse)
                .orElse(null);
    }
}
