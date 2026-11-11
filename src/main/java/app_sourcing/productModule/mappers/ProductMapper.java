package app_sourcing.productModule.mappers;

import app_sourcing.inventory.mappers.InventoryMapper;
import app_sourcing.productModule.dto.ProductDTO;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.entitie.ProductStatus;
import app_sourcing.sellerModule.dto.SellerDTO;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import app_sourcing.sourcingModule.mappers.SourcingRequestMapper;

public class ProductMapper {

    private ProductMapper() {
        // empêche l'instanciation
    }

    /* =========================
       ENTITY -> DTO
       ========================= */
    public static ProductDTO toDTO(Product entity) {
        if (entity == null) {
            return null;
        }

        ProductDTO dto = new ProductDTO();

        dto.setId(entity.getId());
        dto.setNameProduct(entity.getNameProduct());
        dto.setDescriptionProduct(entity.getDescriptionProduct());
        dto.setPrice(entity.getPrice());
        dto.setQteStock(entity.getQteStock());
        dto.setPoidsKg(entity.getPoidsKg());

        if(entity.getRejection()!=null)
            dto.setRejectionReason(entity.getRejection().getReason());

        dto.setFulfillmentMode(
                entity.getFulfillmentMode() != null
                        ? entity.getFulfillmentMode().name()
                        : null
        );

        dto.setCountry(
                entity.getCountry() != null
                        ? entity.getCountry().name()
                        : null
        );

        dto.setStatus(
                entity.getStatus() != null
                        ? entity.getStatus().name()
                        : null
        );

        // Nested objects
        dto.setSeller(
                entity.getSeller() != null
                        ? SellerDTO.fromEntity(entity.getSeller())
                        : null
        );

        dto.setSourcingRequest(
                entity.getSourcingRequest() != null
                        ? SourcingRequestMapper.toResponseDTO(entity.getSourcingRequest())
                        : null
        );

        dto.setInventories(
                entity.getInventories() != null
                        ? entity.getInventories()
                        .stream()
                        .map(InventoryMapper::toDTO)
                        .toList()
                        : null
        );

        return dto;
    }

    /* =========================
       DTO -> ENTITY (creation)
       ========================= */
    public static Product toEntity(
            ProductDTO dto,
            Seller seller,
            SourcingRequest sourcingRequest
    ) {
        if (dto == null) {
            return null;
        }

        Product entity = new Product();

        entity.setId(dto.getId());
        entity.setNameProduct(dto.getNameProduct());
        entity.setDescriptionProduct(dto.getDescriptionProduct());
        entity.setPrice(dto.getPrice());
        entity.setQteStock(dto.getQteStock());

        if (dto.getFulfillmentMode() != null) {
            entity.setFulfillmentMode(
                    FulfillmentMode.valueOf(dto.getFulfillmentMode())
            );
        }

        if (dto.getCountry() != null) {
            entity.setCountry(
                    Country.valueOf(dto.getCountry())
            );
        }

        if (dto.getStatus() != null) {
            entity.setStatus(
                    ProductStatus.valueOf(dto.getStatus())
            );
        }

        entity.setSeller(seller);
        entity.setSourcingRequest(sourcingRequest);
        entity.setIsArchive(false);

        return entity;
    }

    /* =========================
       UPDATE ENTITY FROM DTO
       ========================= */
    public static void updateEntity(Product entity, ProductDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getNameProduct() != null)
            entity.setNameProduct(dto.getNameProduct());

        if (dto.getDescriptionProduct() != null)
            entity.setDescriptionProduct(dto.getDescriptionProduct());

        if (dto.getPrice() != null)
            entity.setPrice(dto.getPrice());

        if (dto.getQteStock() >= 0)
            entity.setQteStock(dto.getQteStock());

        if (dto.getFulfillmentMode() != null)
            entity.setFulfillmentMode(
                    FulfillmentMode.valueOf(dto.getFulfillmentMode())
            );

        if (dto.getCountry() != null)
            entity.setCountry(
                    Country.valueOf(dto.getCountry())
            );

        if (dto.getStatus() != null)
            entity.setStatus(
                    ProductStatus.valueOf(dto.getStatus())
            );
    }
}
