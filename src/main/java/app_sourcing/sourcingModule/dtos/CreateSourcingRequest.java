package app_sourcing.sourcingModule.dtos;

import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class CreateSourcingRequest {

    @NotBlank
    private String nameProduct;
    @NotBlank
    private String description;
    private String linkProduct;
    @NotBlank
    private String variants;
    private Integer estimatedQuantity;
    @NotNull
    private FulfillmentMode fulfillmentMode;
    private Country country;

    // Photos du produit (optionnelles, plusieurs autorisées)
    private List<MultipartFile> images;

}
