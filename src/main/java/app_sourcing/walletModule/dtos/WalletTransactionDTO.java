package app_sourcing.walletModule.dtos;

import app_sourcing.walletModule.entitie.FeeType;
import app_sourcing.walletModule.entitie.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletTransactionDTO {
    private Long id;

    private Long sellerId;
    private String sellerName;
    private String sellerEmail;

    private TransactionType type;
    private FeeType feeType;

    private BigDecimal amount; // signé : + crédit / - débit
    private BigDecimal balanceAfter;

    private String description;

    private String relatedOrderId;
    private Long relatedDepositId;
    private Long relatedWithdrawalId;

    private LocalDateTime createdAt;
}
