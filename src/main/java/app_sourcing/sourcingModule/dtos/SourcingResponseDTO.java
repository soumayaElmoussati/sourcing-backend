package app_sourcing.sourcingModule.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SourcingResponseDTO {
    private String id;
    private String nameProduct;
    private String description;
    private String linkProduct;
    private String variants;
    private Integer estimatedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal localCurrencyPrice;
    private String localCurrencyCode;
    private String fulfillmentMode;
    private String country;
    private String status;
    private List<String> images;
}