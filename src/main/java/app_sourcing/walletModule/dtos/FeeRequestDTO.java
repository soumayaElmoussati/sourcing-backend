package app_sourcing.walletModule.dtos;

import app_sourcing.walletModule.entitie.FeeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeRequestDTO {
    @NotNull
    private FeeType type;

    @NotNull
    @Positive
    private BigDecimal amount;
}
