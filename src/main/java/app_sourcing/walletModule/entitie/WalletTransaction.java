package app_sourcing.walletModule.entitie;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Historique complet des mouvements de solde d'un wallet (le "ledger").
// amount est signé : positif = crédit, négatif = débit.
@Data
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private SellerWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Renseigné uniquement quand type == FEE
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type")
    private FeeType feeType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    private String description;

    // Références optionnelles vers l'objet à l'origine de la transaction
    @Column(name = "related_order_id")
    private String relatedOrderId;

    @Column(name = "related_deposit_id")
    private Long relatedDepositId;

    @Column(name = "related_withdrawal_id")
    private Long relatedWithdrawalId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
