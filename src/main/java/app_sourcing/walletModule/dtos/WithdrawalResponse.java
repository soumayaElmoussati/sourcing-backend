package app_sourcing.walletModule.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WithdrawalResponse {

    private Long id;
    private Double amount;
    private Double feeAmount;
    private Double netAmount;
    private String paymentMethod;
    private String accountDetails;
    private String status;
    private String rejectionReason;
    private String proofDocumentPath;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private WalletInfo walletInfo;

    @Data
    public static class WalletInfo {
        private Long id;
        private Double balance;
        private Long sellerId;
        private String sellerName;
        private String sellerEmail;
    }
}