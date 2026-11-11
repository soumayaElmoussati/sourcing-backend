package app_sourcing.sourcingModule.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SourcingStatsDto {

    private long pending;
    private long approved;
    private long completed;
    private BigDecimal totalAmount;
}
