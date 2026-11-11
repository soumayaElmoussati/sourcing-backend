package app_sourcing.walletModule.entitie;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "deposits")
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private SellerWallet wallet;

    // Montant déclaré par le vendeur au moment du dépôt (à vérifier par l'admin
    // via le justificatif avant confirmation)
    @Column(nullable = false, columnDefinition = "decimal(19,2) default 0")
    private java.math.BigDecimal amount;

    @Column(nullable = false)
    private String paymentMethodType;

    @Column(name = "proof_document_path", nullable = false)
    private String proofDocumentPath;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
