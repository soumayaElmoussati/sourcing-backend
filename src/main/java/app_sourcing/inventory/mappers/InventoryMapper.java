package app_sourcing.inventory.mappers;

import app_sourcing.inventory.dtos.InventoryDTO;
import app_sourcing.inventory.entitie.Inventory;

public class InventoryMapper {

    private InventoryMapper() {}

    public static InventoryDTO toDTO(Inventory entity) {
        if (entity == null) return null;

        InventoryDTO dto = new InventoryDTO();
        dto.setWarehouse(
                entity.getWarehouse() != null
                        ? entity.getWarehouse().name()
                        : null
        );
        dto.setAvailable(entity.getAvailable());
        dto.setReserved(entity.getReserved());
        dto.setReturned(entity.getReturned());

        return dto;
    }
}
