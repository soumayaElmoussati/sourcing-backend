package app_sourcing.sourcingModule.entitie;

import app_sourcing.productModule.entitie.Product;
import app_sourcing.sellerModule.entitie.Seller;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "sourcing_requests")
public class SourcingRequest {

    @Id
    @Column(length = 10, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String nameProduct;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String linkProduct;

    private String variants;

    private Integer estimatedQuantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;          // Prix unitaire en USD (fixé par admin)

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;         // Prix total en USD = unitPrice × quantity

    @Column(precision = 10, scale = 2)
    private BigDecimal localCurrencyPrice; // Prix total en devise locale du pays

    @Column(length = 10)
    private String localCurrencyCode;      // Ex: AED, SAR, KWD...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentMode fulfillmentMode; // 🔒 mandatory

    @Enumerated(EnumType.STRING)
    private Country country; // ✔ required ONLY if LOCAL_SINGLE_COUNTRY


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @OneToOne(mappedBy = "sourcingRequest")
    private Product product;

    @OneToMany(
            mappedBy = "sourcingRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<SourcingRequestImage> images = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isArchive = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}