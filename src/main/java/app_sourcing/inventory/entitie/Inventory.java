package app_sourcing.inventory.entitie;

import app_sourcing.productModule.entitie.Product;
import app_sourcing.sourcingModule.entitie.Country;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"product_id", "warehouse"}
       ))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {

    @Id
    private String id;

    @ManyToOne(optional = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    private Country warehouse;

    private int available;
    private int reserved;
    private int returned;

    private LocalDateTime lastUpdate;
}
