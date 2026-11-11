package app_sourcing.productModule.entitie;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * - Rejected (avec motif)
 * - Motif défini par l’Admin
 * - Historique des rejets conservé
 */
@Entity
@Table(name = "product_rejections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRejection {

    @Id
    @Column(length = 10, nullable = false)
    private String id;

    /**
     * - Chaque rejet est lié à un produit
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "product_id", unique = true)
    private Product product;

    /**
     * - Motif du rejet (obligatoire)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    /**
     * - Rejet effectué par l’Admin
     */
    @Column(nullable = false)
    private String rejectedBy; // adminId ou adminEmail

    /**
     * - Date du rejet (audit)
     */
    @CreationTimestamp
    private Instant rejectedAt;
}


