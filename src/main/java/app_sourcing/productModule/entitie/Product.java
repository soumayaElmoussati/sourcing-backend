package app_sourcing.productModule.entitie;

import app_sourcing.inventory.entitie.Inventory;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 10, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String nameProduct;

    @Column(columnDefinition = "TEXT")
    private String descriptionProduct;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String link;

    private int qteStock;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal poidsKg;

    @Enumerated(EnumType.STRING)
    private FulfillmentMode fulfillmentMode;

    @Enumerated(EnumType.STRING)
    private Country country;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @OneToOne(optional = false)
    @JoinColumn(name = "sourcing_request_id", unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SourcingRequest sourcingRequest;

    /**
     * - Présent uniquement si status = REJECTED
     */
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private ProductRejection rejection;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Inventory> inventories;

    @Column(nullable = false)
    private Boolean isArchive = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void updateStatusAutomatically() {
        if (qteStock <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }

}



