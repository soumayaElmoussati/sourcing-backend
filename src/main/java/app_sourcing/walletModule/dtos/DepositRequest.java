package app_sourcing.walletModule.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class DepositRequest {

    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "1.0", message = "Le montant minimum est de 1 $")
    private BigDecimal amount;

    @NotBlank(message = "Le type de méthode de paiement est requis")
    private String paymentMethodType;

    @NotNull(message = "Le fichier de justificatif est requis")
    private MultipartFile proofDocument;
}
