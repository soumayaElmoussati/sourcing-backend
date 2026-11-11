package app_sourcing.walletModule.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class WithdrawalRequest {

    @NotNull(message = "Le montant est requis")
    @Positive(message = "Le montant doit être positif")
    private Double amount;

    @NotBlank(message = "La méthode de paiement est requise")
    private String paymentMethod;

    @NotBlank(message = "Les détails du compte sont requis")
    private String accountDetails;
}
