package app_sourcing.dashboardAdmin.dtos;

import java.math.BigDecimal;

public record MonthlyStatDto(
        int month,
        int year,
        long orderCount,
        BigDecimal revenue
) {}