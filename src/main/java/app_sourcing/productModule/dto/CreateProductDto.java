package app_sourcing.productModule.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductDto {

    @NotBlank(message = "Request ID est obligatoire")
    private String requestId;


    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    @Digits(integer = 10, fraction = 2, message = "Format prix invalide")
    private BigDecimal price;

    @NotNull(message = "Le poids est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le poids doit être supérieur à 0")
    @Digits(integer = 6, fraction = 2, message = "Format poids invalide")
    private BigDecimal weightKg;

    @NotNull(message = "La quantité en stock est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer stockQuantity;

}
