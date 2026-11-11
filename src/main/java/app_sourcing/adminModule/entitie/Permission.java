package app_sourcing.adminModule.entitie;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(length = 200)
    private String description;

    /** Groupe d'affichage : SELLERS, ORDERS, PRODUCTS, SOURCING, WALLET, SETTINGS */
    @Column(nullable = false, length = 40)
    private String category;
}