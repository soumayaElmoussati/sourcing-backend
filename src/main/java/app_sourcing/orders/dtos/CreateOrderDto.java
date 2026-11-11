package app_sourcing.orders.dtos;

import app_sourcing.orders.entities.PaymentMode;
import app_sourcing.sourcingModule.entitie.Country;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderDto {

    private String productId;

    private String customerName;
    private String customerPhone;

    private Country country;
    private String city;
    private String fullAddress;
    private String note;

    private Integer quantity;
    private BigDecimal unitPrice;

    private PaymentMode paymentMode;
}
