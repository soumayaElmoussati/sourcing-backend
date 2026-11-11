package app_sourcing.orders.dtos;

import app_sourcing.orders.entities.OrderStatus;
import app_sourcing.orders.entities.PaymentMode;
import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import app_sourcing.orders.entities.ShippingType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponseDto {

    private String id;

    // 🔹 references (IDs only)
    private Long sellerId;
    private String productId;

    // 🔹 customer
    private String customerName;
    private String customerPhone;

    // 🔹 address
    private Country country;
    private String city;
    private String fullAddress;
    private String note;

    // 🔹 order data
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    private PaymentMode paymentMode;
    private OrderStatus status;

    // 🔹 inherited / auto
    private FulfillmentMode fulfillmentMode;
    private ShippingType shippingType;
    private BigDecimal vatAmount;

    private LocalDateTime createdAt;

    // 🔹 intégrations externes
    private app_sourcing.orders.entities.OrderSource source;
    private String externalOrderId;
}
