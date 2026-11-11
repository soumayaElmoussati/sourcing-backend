package app_sourcing.walletModule.entitie;

import app_sourcing.sellerModule.entitie.Seller;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Compte de paiement enregistré par le vendeur (Binance, virement bancaire...)
// pour être réutilisé rapidement lors d'une demande de retrait.
@Data
@Entity
@Table(name = "seller_payment_methods")
public class SellerPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    // BINANCE ou BANK_TRANSFER
    @Column(nullable = false)
    private String methodType;

    // Libellé affiché (ex: "Binance", "Banque Populaire")
    @Column(nullable = false)
    private String label;

    // PayID/email Binance, ou IBAN/RIB pour une banque
    @Column(nullable = false)
    private String accountDetails;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
