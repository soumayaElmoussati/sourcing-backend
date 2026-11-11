package app_sourcing.productModule.dto;

import app_sourcing.inventory.dtos.InventoryDTO;
import app_sourcing.sellerModule.dto.SellerDTO;
import app_sourcing.sourcingModule.dtos.SourcingResponseDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private String id;
    private String nameProduct;
    private String descriptionProduct;
    private BigDecimal price;
    private int qteStock;
    private BigDecimal poidsKg;
    private String fulfillmentMode;
    private String country;
    private String status;
    private String rejectionReason;
    private SellerDTO seller; // nested DTO
    private SourcingResponseDTO sourcingRequest; // nested DTO
    private List<InventoryDTO> inventories;
}
