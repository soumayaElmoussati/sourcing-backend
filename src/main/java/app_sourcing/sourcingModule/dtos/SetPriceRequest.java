package app_sourcing.sourcingModule.dtos;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SetPriceRequest {
    /** Prix unitaire en USD (par unité, fixé par l'admin) */
    private BigDecimal unitPrice;

    /** Prix total en devise locale du pays de livraison */
    private BigDecimal localCurrencyPrice;

    /** Code devise locale (AED, SAR, KWD, QAR, OMR, BHD, IQD) */
    private String localCurrencyCode;
}