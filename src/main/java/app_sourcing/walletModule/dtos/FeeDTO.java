package app_sourcing.walletModule.dtos;

import app_sourcing.walletModule.entitie.FeeType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeDTO {
    private Long id;
    private FeeType type;
    private BigDecimal amount;
    private String updatedAt;
}
