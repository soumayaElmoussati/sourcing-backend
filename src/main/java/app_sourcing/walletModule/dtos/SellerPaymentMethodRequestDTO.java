package app_sourcing.walletModule.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SellerPaymentMethodRequestDTO {
    @NotBlank
    private String methodType;

    @NotBlank
    private String label;

    @NotBlank
    private String accountDetails;
}
