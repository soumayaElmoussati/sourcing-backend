package app_sourcing.dashboardAdmin.dtos;

import java.math.BigDecimal;

public record TopProductDto(
        String name,
        long orderCount,
        BigDecimal revenue
) {}