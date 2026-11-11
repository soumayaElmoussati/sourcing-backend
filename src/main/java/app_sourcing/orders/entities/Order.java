package app_sourcing.orders.entities;

import app_sourcing.productModule.entitie.Product;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @Column(length = 10, nullable = false, updatable = false)
    private String id;

    @ManyToOne(optional = false)
    private Seller seller;

    @ManyToOne(optional = false)
    private Product product;

    @OneToOne(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            optional = false
    )
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private Country country; // contrôlée par Fulfillment Mode

    private String note;

    // ================= ORDER DATA =================
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // ================= AUTO (HERITED) =================
    @Enumerated(EnumType.STRING)
    private FulfillmentMode fulfillmentMode;

    //    @Enumerated(EnumType.STRING)
//    private Warehouse warehouse;
//
    @Enumerated(EnumType.STRING)
    private ShippingType shippingType;

    private BigDecimal vatAmount;

    private LocalDateTime createdAt;

    // ================= INTEGRATIONS EXTERNES =================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSource source = OrderSource.INTERNAL;

    // Identifiant de la commande côté plateforme externe (évite les doublons
    // en cas de webhook renvoyé plusieurs fois). Null pour les commandes internes.
    @Column(name = "external_order_id")
    private String externalOrderId;
}

