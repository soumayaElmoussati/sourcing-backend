package app_sourcing.sourcingModule.mappers;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.shareClasses.IdGenerator;
import app_sourcing.sourcingModule.dtos.CreateSourcingRequest;
import app_sourcing.sourcingModule.dtos.SourcingResponseDTO;
import app_sourcing.sourcingModule.entitie.RequestStatus;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import app_sourcing.sourcingModule.entitie.SourcingRequestImage;

public class SourcingRequestMapper {

    private SourcingRequestMapper() {
        // empêche l'instanciation
    }

    public static SourcingResponseDTO toResponseDTO(SourcingRequest entity) {
        if (entity == null) {
            return null;
        }

        SourcingResponseDTO dto = new SourcingResponseDTO();

        dto.setId(entity.getId());
        dto.setNameProduct(entity.getNameProduct());
        dto.setDescription(entity.getDescription());
        dto.setLinkProduct(entity.getLinkProduct());
        dto.setVariants(entity.getVariants());
        dto.setEstimatedQuantity(entity.getEstimatedQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setLocalCurrencyPrice(entity.getLocalCurrencyPrice());
        dto.setLocalCurrencyCode(entity.getLocalCurrencyCode());

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

        dto.setImages(
                entity.getImages() != null
                        ? entity.getImages().stream().map(SourcingRequestImage::getImagePath).toList()
                        : java.util.List.of()
        );

        return dto;
    }

    public static SourcingRequest toEntity(
            CreateSourcingRequest request,
            Seller seller
    ) {
        if (request == null) {
            return null;
        }

        SourcingRequest entity = new SourcingRequest();

        entity.setId(IdGenerator.generateId("SR"));
        entity.setNameProduct(request.getNameProduct());
        entity.setDescription(request.getDescription());
        entity.setLinkProduct(request.getLinkProduct());
        entity.setVariants(request.getVariants());
        entity.setEstimatedQuantity(request.getEstimatedQuantity());

        entity.setFulfillmentMode(request.getFulfillmentMode());
        entity.setCountry(request.getCountry());

        entity.setStatus(RequestStatus.PENDING);
        entity.setSeller(seller);
        entity.setIsArchive(false);

        return entity;
    }

}