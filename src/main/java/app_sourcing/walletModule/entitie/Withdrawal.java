package app_sourcing.walletModule.entitie;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "withdrawals")
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private SellerWallet wallet;

    @Column(nullable = false)
    private Double amount;      // Montant brut demandé

    @Column(name = "fee_amount")
    private Double feeAmount;   // Frais 2%

    @Column(name = "net_amount")
    private Double netAmount;   // Montant net reçu (amount - feeAmount)

    @Column(nullable = false)
    private String paymentMethod; // BANK_TRANSFER, PAYPAL, WAVE, ORANGE_MONEY, etc.

    @Column(name = "account_details", nullable = false, length = 500)
    private String accountDetails; // Détails du compte de réception

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, REJECTED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Justificatif de paiement envoyé par l'admin (preuve qu'il a payé le vendeur)
    @Column(name = "proof_document_path")
    private String proofDocumentPath;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}