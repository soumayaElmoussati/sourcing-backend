package app_sourcing.inventory.dtos;

import lombok.Data;

@Data
public class InventoryDTO {
    private String warehouse;
    private int available;
    private int reserved;
    private int returned;
}
