package app_sourcing.walletModule.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositAdminDTO {
    private Long id;
    private Long walletId;
    private Long sellerId;
    private String sellerName;
    private String sellerEmail;
    private String paymentMethodType;
    private String proofDocumentPath;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double walletBalance;
}
