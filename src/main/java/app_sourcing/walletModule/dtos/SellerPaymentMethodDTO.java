package app_sourcing.walletModule.dtos;

import lombok.Data;

@Data
public class SellerPaymentMethodDTO {
    private Long id;
    private String methodType;
    private String label;
    private String accountDetails;
    private String createdAt;
}
